"""Check an existing Godot project with the engine installed on the runner."""
import argparse
import shutil
import subprocess
import sys
from pathlib import Path

def validate(path, binary="godot", timeout=180):
    root=Path(path).expanduser().resolve()
    if not (root/"project.godot").is_file():
        raise ValueError("project.godot não encontrado em "+str(root))
    exe=shutil.which(binary)
    if not exe:
        raise RuntimeError("Godot não está instalado no runner: "+binary)
    cmd=[exe,"--headless","--path",str(root),"--editor","--quit"]
    p=subprocess.run(cmd,capture_output=True,text=True,timeout=timeout)
    output=(p.stdout+"\n"+p.stderr).strip()
    if p.returncode:
        raise RuntimeError("Godot retornou erro "+str(p.returncode)+"\n"+output[-5000:])
    return output

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("path",help="Diretório com project.godot")
    parser.add_argument("--godot",default="godot",help="Executável Godot disponível no PATH")
    args=parser.parse_args()
    try:
        print(validate(args.path,args.godot))
    except (RuntimeError,ValueError,subprocess.TimeoutExpired) as exc:
        print("ERRO:",exc,file=sys.stderr)
        raise SystemExit(1)

if __name__=="__main__":
    main()
