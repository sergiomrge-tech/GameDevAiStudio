import unittest
from unittest.mock import patch
from studio.github_issue import MARKER,plan,handle

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
if __name__=="__main__":unittest.main()
