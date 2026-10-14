"""GameDev AI Studio: persistent project/task domain and agent orchestration."""
import json
import os
import re
import shutil
import sqlite3
import subprocess
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path

AGENTS = [
    {"id":"director","name":"Diretor de Desenvolvimento","focus":"architecture, scope, acceptance criteria","keywords":[]},
    {"id":"programmer","name":"Programador Sênior","focus":"Godot GDScript, Unity C#, code quality","keywords":["código","script","bug","erro","godot","unity","programar","code"]},
    {"id":"artist","name":"Diretor Artístico","focus":"assets, shaders, lighting, consistency","keywords":["visual","arte","textura","modelo","árvore","cenario","cenário","animação","blender"]},
    {"id":"gameplay","name":"Especialista em Gameplay","focus":"combat, balancing, controls, progression","keywords":["combate","jogabilidade","inimigo","movimento","inventário","skill","fase"]},
    {"id":"qa","name":"Testador e QA","focus":"reproducible tests, visual QA, performance","keywords":["teste","validar","fps","crash","corrigir","desempenho"]},
    {"id":"build","name":"Engenheiro de Builds","focus":"GitHub, CI, Android/PC packaging","keywords":["apk","exportar","build","compilar","github","publicar"]},
]
GITHUB_RE = re.compile(r"^https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\.git)?$")


def now():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def choose_agent(text):
    content = text.casefold()
    scores = [(sum((4 if word in {"apk","compilar","build","exportar"} else 1) for word in a["keywords"] if word in content), a["id"]) for a in AGENTS[1:]]
    winner = max(scores, default=(0,"director"), key=lambda s:s[0])
    return winner[1] if winner[0] else "director"


class Studio:
    def __init__(self, db_path, workspace):
        self.db_path = Path(db_path).expanduser().resolve()
        self.workspace = Path(workspace).expanduser().resolve()
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.workspace.mkdir(parents=True, exist_ok=True)
        with self.connection() as conn:
            conn.executescript("""
            CREATE TABLE IF NOT EXISTS projects (
                id TEXT PRIMARY KEY, name TEXT NOT NULL, repository TEXT NOT NULL,
                local_path TEXT NOT NULL, created_at TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY, project_id TEXT NOT NULL, title TEXT NOT NULL,
                description TEXT NOT NULL, agent_id TEXT NOT NULL,
                status TEXT NOT NULL, plan TEXT NOT NULL, log TEXT NOT NULL,
                branch TEXT NOT NULL, created_at TEXT NOT NULL,
                FOREIGN KEY (project_id) REFERENCES projects(id));
            """)

    def connection(self):
        conn = sqlite3.connect(self.db_path, timeout=10)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys=ON")
        return conn

    def projects(self):
        with self.connection() as conn:
            return [dict(row) for row in conn.execute("SELECT * FROM projects ORDER BY created_at DESC")]

    def tasks(self, project_id=None):
        with self.connection() as conn:
            if project_id:
                rows = conn.execute("SELECT * FROM tasks WHERE project_id=? ORDER BY created_at DESC", (project_id,))
            else:
                rows = conn.execute("SELECT * FROM tasks ORDER BY created_at DESC")
            return [dict(row) for row in rows]

    def project(self, project_id):
        with self.connection() as conn:
            row = conn.execute("SELECT * FROM projects WHERE id=?", (project_id,)).fetchone()
        if not row:
            raise LookupError("Projeto não encontrado")
        return dict(row)

    def task(self, task_id):
        with self.connection() as conn:
            row = conn.execute("SELECT * FROM tasks WHERE id=?", (task_id,)).fetchone()
        if not row:
            raise LookupError("Tarefa não encontrada")
        return dict(row)

    def add_project(self, name, repository="", local_path=""):
        name = str(name).strip()
        repository = str(repository).strip()
        if not 2 <= len(name) <= 90:
            raise ValueError("Nome do projeto deve ter entre 2 e 90 caracteres")
        if repository and not GITHUB_RE.fullmatch(repository):
            raise ValueError("Informe uma URL HTTPS de repositório GitHub")
        if local_path:
            dest = Path(local_path).expanduser().resolve()
        else:
            slug = re.sub(r"[^a-z0-9_-]+","-",name.lower()).strip("-")[:40] or "project"
            dest = (self.workspace / slug).resolve()
        if not dest.is_relative_to(self.workspace):
            raise ValueError("Caminho local deve estar dentro de STUDIO_WORKSPACE")
        item = {"id":uuid.uuid4().hex,"name":name,"repository":repository,"local_path":str(dest),"created_at":now()}
        with self.connection() as conn:
            conn.execute("INSERT INTO projects VALUES (:id,:name,:repository,:local_path,:created_at)",item)
        return item

    def add_task(self, project_id, title, description=""):
        self.project(project_id)
        title = str(title).strip()
        description = str(description).strip()
        if not 3 <= len(title) <= 180 or len(description) > 8000:
            raise ValueError("Título inválido ou descrição muito longa")
        item={"id":uuid.uuid4().hex,"project_id":project_id,"title":title,
              "description":description,"agent_id":choose_agent(title+" "+description),
              "status":"new","plan":"","log":"","branch":"","created_at":now()}
        with self.connection() as conn:
            conn.execute("""INSERT INTO tasks VALUES (:id,:project_id,:title,:description,:agent_id,
                         :status,:plan,:log,:branch,:created_at)""", item)
        return item

    def update_task(self, task_id, **changes):
        allowed={"status","plan","log","branch"}
        if set(changes)-allowed:
            raise ValueError("Campo de alteração não permitido")
        self.task(task_id)
        fields=",".join(k+"=?" for k in changes)
        if changes:
            with self.connection() as conn:
                conn.execute("UPDATE tasks SET "+fields+" WHERE id=?",[*changes.values(),task_id])
        return self.task(task_id)

    def instructions(self, task):
        agent=next(a for a in AGENTS if a["id"]==task["agent_id"])
        project=self.project(task["project_id"])
        return (
            "Você é "+agent["name"]+" de um estúdio profissional de jogos. "
            "Especialidade: "+agent["focus"]+". "
            "Projeto: "+project["name"]+". Solicitação: "+task["title"]+". "
            "Detalhes: "+task["description"]+". "
            "Preserve conteúdo existente, não apague assets sem justificativa, "
            "não altere identidade artística aprovada, não faça push ou merge, "
            "não afirme testes não executados. "
            "Descreva arquivos, critérios de aceite e testes concretos."
        )

    def plan_task(self, task_id):
        task=self.task(task_id)
        key=os.getenv("OPENAI_API_KEY")
        model=os.getenv("OPENAI_MODEL") or "gpt-5-mini"
        if not key:
            raise RuntimeError("Planejamento com IA requer OPENAI_API_KEY no servidor")
        payload=json.dumps({
            "model":model, "store":False,
            "instructions":"Elabore um plano curto e executável de desenvolvimento de jogos em português. Não declare implementação concluída.",
            "input":self.instructions(task)+"\nResponda com objetivo, etapas, arquivos, testes e riscos."
        }).encode("utf-8")
        req=urllib.request.Request("https://api.openai.com/v1/responses",data=payload,
            headers={"Authorization":"Bearer "+key,"Content-Type":"application/json"},method="POST")
        try:
            with urllib.request.urlopen(req,timeout=90) as response:
                result=json.load(response)
        except urllib.error.HTTPError as exc:
            raise RuntimeError("API OpenAI retornou HTTP "+str(exc.code)) from exc
        output=[]
        for chunk in result.get("output",[]):
            for part in chunk.get("content",[]):
                if part.get("type")=="output_text":
                    output.append(part.get("text",""))
        plan="\n".join(output).strip()
        if not plan:
            raise RuntimeError("API não retornou um plano textual")
        return self.update_task(task_id,plan=plan,status="planned")

    def run_task(self, task_id):
        if os.getenv("ENABLE_CODEX_EXEC")!="1":
            raise RuntimeError("Executor desativado: configure ENABLE_CODEX_EXEC=1 em runner isolado")
        if not os.getenv("STUDIO_TOKEN"):
            raise RuntimeError("STUDIO_TOKEN é obrigatório para habilitar execução")
        task=self.task(task_id)
        if task["status"] not in ("new","planned","failed"):
            raise ValueError("Estado da tarefa não permite execução")
        project=self.project(task["project_id"])
        path=Path(project["local_path"]).resolve()
        if not path.is_relative_to(self.workspace) or not (path/".git").exists():
            raise RuntimeError("Repositório Git local não encontrado dentro de STUDIO_WORKSPACE")
        if shutil.which("git") is None or shutil.which("codex") is None:
            raise RuntimeError("Runner precisa de git e Codex CLI autenticado")
        def git(*args):
            return subprocess.run(["git","-C",str(path),*args],capture_output=True,text=True,timeout=20,check=True).stdout.strip()
        if git("status","--porcelain"):
            raise RuntimeError("Repositório possui alterações locais; execute em checkout limpo")
        branch="studio/task-"+task_id[:12]
        if branch in git("branch","--list",branch).split():
            raise RuntimeError("Branch de trabalho já existe")
        git("switch","-c",branch)
        self.update_task(task_id,status="running",branch=branch)
        prompt=self.instructions(task)+"\nImplemente as mudanças neste repositório. Execute testes pertinentes. Não faça commit, push nem merge. "
        try:
            result=subprocess.run(["codex","exec","--cd",str(path),"--sandbox","workspace-write",prompt],
                cwd=path,capture_output=True,text=True,timeout=900)
            log=(result.stdout+"\n"+result.stderr)[-16000:]
            return self.update_task(task_id,status="review" if result.returncode==0 else "failed",log=log)
        except (subprocess.TimeoutExpired,OSError) as exc:
            return self.update_task(task_id,status="failed",log=str(exc))
