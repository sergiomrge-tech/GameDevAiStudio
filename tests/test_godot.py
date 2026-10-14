import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from scripts.check_godot import validate


class GodotBridgeTests(unittest.TestCase):
    def test_requires_project(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError,"project.godot"):
                validate(tmp)

    def test_missing_engine_is_explicit(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp,"project.godot").write_text("[application]\n")
            with patch("scripts.check_godot.shutil.which",return_value=None):
                with self.assertRaisesRegex(RuntimeError,"não está instalado"):
                    validate(tmp)

    def test_expected_headless_command(self):
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp,"project.godot").write_text("[application]\n")
            class Result:
                returncode=0
                stdout="All good"
                stderr=""
            with patch("scripts.check_godot.shutil.which",return_value="/usr/bin/godot"):
                with patch("scripts.check_godot.subprocess.run",return_value=Result()) as run:
                    self.assertEqual(validate(tmp),"All good")
                    self.assertEqual(run.call_args.args[0][:3],["/usr/bin/godot","--headless","--path"])


if __name__=="__main__":
    unittest.main()
