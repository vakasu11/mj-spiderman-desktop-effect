"""Run with Windows Python: python build_windows.py [output-directory]."""
from pathlib import Path
import subprocess
import sys
import tkinter

# Fail early instead of producing an EXE missing the GUI runtime.
tkinter.Tcl().eval('info library')

root = Path(__file__).resolve().parent
app = root / '桌面特效'
dest = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else root / 'dist'
cmd = [sys.executable, '-m', 'PyInstaller', '--noconfirm', '--clean', '--onefile', '--windowed',
       '--name', 'mj-spiderman', '--distpath', str(dest),
       '--workpath', str(root / 'work' / 'build'), '--specpath', str(root / 'work'),
       '--icon', str(root / 'assets/app.ico'), '--hidden-import', 'pynput.keyboard._win32',
       '--hidden-import', 'pynput.mouse._win32']
cmd += ['--add-data', f'{root / "assets"}:assets']
for name in ['frames', 'frames2', 'audio.wav', 'audio2.wav', 'icon.ico']:
    cmd += ['--add-data', f'{app / name}:{name if name.startswith("frames") else "."}']
cmd += [str(app / '蜘蛛侠桌面特效.pyw')]
subprocess.run(cmd, check=True, cwd=root)
