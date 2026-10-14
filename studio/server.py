"""Local HTTP API and mobile dashboard. No third-party runtime dependencies."""
import hmac
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs
from studio.core import AGENTS, Studio

ROOT=Path(__file__).resolve().parent.parent
DB=os.getenv("STUDIO_DB",str(ROOT/"data"/"studio.sqlite"))
WORKSPACE=os.getenv("STUDIO_WORKSPACE",str(ROOT/"workspace"))
APP=Studio(DB,WORKSPACE)
INDEX=(ROOT/"web"/"index.html").read_bytes()


class Handler(BaseHTTPRequestHandler):
    def reply(self, code, payload):
        data=json.dumps(payload,ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type","application/json; charset=utf-8")
        self.send_header("Content-Length",str(len(data)))
        self.send_header("Cache-Control","no-store")
        self.send_header("X-Content-Type-Options","nosniff")
        self.end_headers()
        self.wfile.write(data)

    def authorized(self):
        expected=os.getenv("STUDIO_TOKEN","")
        supplied=self.headers.get("X-Studio-Token","")
        return (not expected) or hmac.compare_digest(supplied,expected)

    def do_GET(self):
        path=urlparse(self.path).path
        if path=="/":
            self.send_response(200)
            self.send_header("Content-Type","text/html; charset=utf-8")
            self.send_header("Content-Length",str(len(INDEX)))
            self.send_header("X-Content-Type-Options","nosniff")
            self.send_header("Cache-Control","no-store")
            self.end_headers()
            self.wfile.write(INDEX)
            return
        if not self.authorized():
            return self.reply(401,{"error":"Token inválido"})
        if path=="/api/health":
            return self.reply(200,{"ok":True,"version":"0.1.0","ai_ready":bool(os.getenv("OPENAI_API_KEY") and os.getenv("OPENAI_MODEL")),
                "executor_ready":os.getenv("ENABLE_CODEX_EXEC")=="1"})
        if path=="/api/agents":
            return self.reply(200,[{k:v for k,v in a.items() if k!="keywords"} for a in AGENTS])
        if path=="/api/projects":
            return self.reply(200,APP.projects())
        if path=="/api/tasks":
            project_id=parse_qs(urlparse(self.path).query).get("project_id",[None])[0]
            return self.reply(200,APP.tasks(project_id))
        if path.startswith("/api/tasks/"):
            try:
                return self.reply(200,APP.task(path.split("/")[3]))
            except LookupError as exc:
                return self.reply(404,{"error":str(exc)})
        return self.reply(404,{"error":"Rota não encontrada"})

    def do_POST(self):
        if not self.authorized():
            return self.reply(401,{"error":"Token inválido"})
        size=int(self.headers.get("Content-Length","0") or "0")
        if size<1 or size>16384:
            return self.reply(413,{"error":"JSON vazio ou muito grande"})
        try:
            payload=json.loads(self.rfile.read(size))
            if not isinstance(payload,dict):
                raise ValueError("Envie um objeto JSON")
            path=urlparse(self.path).path
            if path=="/api/projects":
                return self.reply(201,APP.add_project(payload.get("name",""),
                    payload.get("repository",""),payload.get("local_path","")))
            if path=="/api/tasks":
                return self.reply(201,APP.add_task(payload.get("project_id",""),
                    payload.get("title",""),payload.get("description","")))
            parts=path.strip("/").split("/")
            if len(parts)==4 and parts[:2]==["api","tasks"]:
                task_id,action=parts[2],parts[3]
                if action=="plan":
                    return self.reply(200,APP.plan_task(task_id))
                if action=="run":
                    return self.reply(200,APP.run_task(task_id))
            return self.reply(404,{"error":"Rota não encontrada"})
        except (ValueError,TypeError) as exc:
            return self.reply(400,{"error":str(exc)})
        except LookupError as exc:
            return self.reply(404,{"error":str(exc)})
        except RuntimeError as exc:
            return self.reply(409,{"error":str(exc)})
        except Exception:
            self.log_error("Unhandled request exception")
            return self.reply(500,{"error":"Falha interna. Consulte os logs do servidor."})


def main():
    host=os.getenv("STUDIO_HOST","127.0.0.1")
    port=int(os.getenv("STUDIO_PORT","8765"))
    if host not in ("127.0.0.1","localhost","::1") and not os.getenv("STUDIO_TOKEN"):
        raise SystemExit("Para escutar fora de localhost, defina STUDIO_TOKEN e configure HTTPS no proxy.")
    print("GameDev AI Studio v0.1: http://"+host+":"+str(port),flush=True)
    ThreadingHTTPServer((host,port),Handler).serve_forever()


if __name__=="__main__":
    main()
