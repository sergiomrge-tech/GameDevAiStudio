import json
import os
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path
from unittest.mock import patch

from studio.core import AGENTS, Studio, choose_agent


class StudioTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        root=Path(self.tmp.name)
        self.studio=Studio(root/"db.sqlite",root/"workspace")

    def tearDown(self):
        self.tmp.cleanup()

    def test_agents_and_routing(self):
        self.assertEqual(len(AGENTS),6)
        self.assertEqual(choose_agent("Corrigir bug no código Godot"),"programmer")
        self.assertEqual(choose_agent("Criar animação da árvore no Blender"),"artist")
        self.assertEqual(choose_agent("Gerar APK Android"),"build")
        self.assertEqual(choose_agent("Criar novo projeto sem especificação"),"director")

    def test_persistence_and_project_link(self):
        p=self.studio.add_project("Dungeons Fire","https://github.com/owner/dungeons-fire")
        t=self.studio.add_task(p["id"],"Melhorar combate","Revisar movimentação do herói")
        self.assertEqual(self.studio.task(t["id"])["status"],"new")
        self.assertEqual(self.studio.task(t["id"])["agent_id"],"gameplay")
        self.assertEqual(len(self.studio.projects()),1)
        reopened=Studio(self.studio.db_path,self.studio.workspace)
        self.assertEqual(reopened.tasks()[0]["title"],"Melhorar combate")
        reopened.update_task(t["id"],status="planned",plan="Testar")
        self.assertEqual(reopened.task(t["id"])["plan"],"Testar")

    def test_reject_outside_workspace_and_bad_links(self):
        with self.assertRaises(ValueError):
            self.studio.add_project("Outside",local_path=str(Path(self.tmp.name)/"other"))
        with self.assertRaises(ValueError):
            self.studio.add_project("Bad link",repository="https://attacker.example/repo")
        with self.assertRaises(LookupError):
            self.studio.add_task("missing","Nova tarefa")
        with self.assertRaises(ValueError):
            self.studio.add_project("")

    def test_agent_instruction_and_disabled_executor(self):
        p=self.studio.add_project("Projeto")
        t=self.studio.add_task(p["id"],"Corrigir bug")
        self.assertIn("não faça push ou merge",self.studio.instructions(t))
        with patch.dict(os.environ,{"ENABLE_CODEX_EXEC":"0"},clear=False):
            with self.assertRaisesRegex(RuntimeError,"Executor desativado"):
                self.studio.run_task(t["id"])
        with patch.dict(os.environ,{"OPENAI_API_KEY":"","OPENAI_MODEL":""},clear=False):
            with self.assertRaisesRegex(RuntimeError,"OPENAI_API_KEY"):
                self.studio.plan_task(t["id"])


class APITests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import studio.server as server
        cls.server_module=server
        cls.tmp=tempfile.TemporaryDirectory()
        cls.previous=server.APP
        server.APP=Studio(Path(cls.tmp.name)/"state.sqlite",Path(cls.tmp.name)/"workspace")
        cls.http=ThreadingHTTPServer(("127.0.0.1",0),server.Handler)
        cls.thread=threading.Thread(target=cls.http.serve_forever,daemon=True)
        cls.thread.start()
        cls.base="http://127.0.0.1:"+str(cls.http.server_port)

    @classmethod
    def tearDownClass(cls):
        cls.http.shutdown()
        cls.http.server_close()
        cls.thread.join(timeout=3)
        cls.server_module.APP=cls.previous
        cls.tmp.cleanup()

    def call(self,path,method="GET",body=None,token=""):
        data=json.dumps(body).encode("utf-8") if body is not None else None
        req=urllib.request.Request(self.base+path,data=data,method=method,
            headers={"Content-Type":"application/json","X-Studio-Token":token})
        return urllib.request.urlopen(req,timeout=4)

    def test_health_and_crud(self):
        with patch.dict(os.environ,{"STUDIO_TOKEN":""},clear=False):
            health=json.load(self.call("/api/health"))
            self.assertTrue(health["ok"])
            p=json.load(self.call("/api/projects","POST",{"name":"Meu Jogo"}))
            t=json.load(self.call("/api/tasks","POST",{"project_id":p["id"],"title":"Criar mapa da ilha"}))
            self.assertEqual(t["agent_id"],"director")
            self.assertEqual(json.load(self.call("/api/tasks"))[0]["id"],t["id"])
            self.assertEqual(json.load(self.call("/api/tasks/"+t["id"]))["title"],t["title"])

    def test_authentication(self):
        with patch.dict(os.environ,{"STUDIO_TOKEN":"secret-test"},clear=False):
            with self.assertRaises(urllib.error.HTTPError) as ctx:
                self.call("/api/projects")
            self.assertEqual(ctx.exception.code,401)
            self.assertIsInstance(json.load(self.call("/api/projects",token="secret-test")),list)

    def test_validation(self):
        with patch.dict(os.environ,{"STUDIO_TOKEN":""},clear=False):
            with self.assertRaises(urllib.error.HTTPError) as ctx:
                self.call("/api/projects","POST",{"name":"x"})
            self.assertEqual(ctx.exception.code,400)


if __name__=="__main__":
    unittest.main()
