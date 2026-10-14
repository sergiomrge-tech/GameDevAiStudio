import unittest
from unittest.mock import patch
import io
import json
import urllib.error
from studio.github_issue import MARKER,DEFAULT_MODEL,plan,handle

class GitHubIssueTests(unittest.TestCase):
    def test_without_key_never_pretends_to_plan(self):
        message=plan("Gerar APK","Godot")
        self.assertIn(MARKER,message)
        self.assertIn("Engenheiro de Builds",message)
        self.assertIn("Planejamento IA pendente",message)
    def test_no_non_owner(self):
        event={"issue":{"number":2,"title":"Bug","body":"","user":{"login":"outsider"}}}
        with self.assertRaises(PermissionError):
            handle(event,"owner/repo","token")
    def test_publish_comment(self):
        event={"issue":{"number":1,"title":"Corrigir código Godot","body":"","user":{"login":"owner"}}}
        with patch("studio.github_issue.api",side_effect=[[],{"html_url":"https://github.com/comment"}]) as api:
            result=handle(event,"owner/repo","token")
            self.assertEqual(result["agent"],"programmer")
            self.assertEqual(api.call_count,2)
            self.assertIn(MARKER,api.call_args.args[2]["body"])
    def test_skip_duplicate(self):
        event={"issue":{"number":1,"title":"Criar","body":"","user":{"login":"owner"}}}
        with patch("studio.github_issue.api",return_value=[{"body":MARKER}]) as api:
            self.assertEqual(handle(event,"owner/repo","token")["skipped"],"already processed")
            self.assertEqual(api.call_count,1)
    def test_model_default_applied_with_key(self):
        event={"issue":{"number":8,"title":"Melhorar código Godot","body":"Revisar combate","user":{"login":"owner"}}}
        with patch("studio.github_issue.api",side_effect=[[],{"html_url":"https://github.com/comment"}]):
            with patch("studio.github_issue.plan",return_value="Planejado") as mocked:
                result=handle(event,"owner/repo","token","sk-test-placeholder",model="")
        self.assertIn("comment",result)
        self.assertEqual(mocked.call_args.args[3],DEFAULT_MODEL)

    def test_pending_issue_is_reprocessed_after_key(self):
        event={"issue":{"number":8,"title":"Corrigir jogo","body":"Revisar","user":{"login":"owner"}}}
        with patch("studio.github_issue.api",side_effect=[
            [{"body":MARKER+"\n**Planejamento IA pendente.**"}],{"html_url":"https://github.com/comment"}]):
            with patch("studio.github_issue.plan",return_value="Novo plano"):
                result=handle(event,"owner/repo","token","sk-test-placeholder","")
        self.assertIn("comment",result)

    def test_api_billing_error_is_explained_without_secrets(self):
        event={"issue":{"number":9,"title":"Corrigir jogo","body":"","user":{"login":"owner"}}}
        error=urllib.error.HTTPError("https://api.openai.com/v1/responses",429,"Too Many Requests",{},io.BytesIO())
        with patch("studio.github_issue.api",side_effect=[[],{"html_url":"https://github.com/comment"}]) as mocked:
            with patch("studio.github_issue.plan",side_effect=error):
                result=handle(event,"owner/repo","token","sk-secret-sample","")
        self.assertEqual(result["ai_error_http"],429)
        body=mocked.call_args.args[2]["body"]
        self.assertIn("créditos",body)
        self.assertNotIn("sk-secret-sample",body)
if __name__=="__main__":unittest.main()
