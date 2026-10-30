"""Stage the current engine/UI with Android-specific platform adapters, never user data."""
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEST = ROOT / "android/app/src/main/python"
DEST.mkdir(parents=True, exist_ok=True)
shutil.copytree(ROOT / "nathan", DEST / "nathan", dirs_exist_ok=True,
                ignore=shutil.ignore_patterns("__pycache__", "*.pyc", "*.sqlite", "*.db"))
for path in (DEST / "nathan").rglob("*.py"):
    text = path.read_text(encoding="utf-8").replace("import httpx2 as httpx", "import httpx")
    path.write_text(text, encoding="utf-8")
shutil.copyfile(ROOT / "providers.yaml", DEST / "nathan/providers.yaml")
config = DEST / "nathan/config.py"
config.write_text(config.read_text().replace('DEFAULT_CONFIG = ROOT / "providers.yaml"',
                  'DEFAULT_CONFIG = Path(__file__).parent / "providers.yaml"'))
for path in (ROOT / "android/python").rglob("*.py"):
    target = DEST / path.relative_to(ROOT / "android/python")
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, target)
# Native desktop tools must not claim to control another device from an Android APK.
(DEST / "nathan/tools/desktop.py").write_text('def make_tools():\n    return []\n')
(DEST / "nathan/tools/python_exec.py").write_text('''from nathan.tools.base import Tool, params
def make_tool(workspace=None):
    return Tool("python", "Desktop-only Python subprocess tool (unavailable on Android)",
                params(code=("string", "code")),
                lambda code: "Python subprocess tools are not available on Android.")
''')
# Keep the upstream interface, with a compact navigation bar for phone screens.
css = DEST / "nathan/web/static/style.css"
with css.open("a", encoding="utf-8") as stream:
    stream.write('''
@media (max-width: 700px) {
  .rail { position: fixed; inset: auto 0 0; width: 100%; height: 58px; display: flex;
    overflow-x: auto; z-index: 20; background: #030810; }
  .rail a { flex: 0 0 62px; }
  main { margin-left: 0; padding-bottom: 70px; }
  .world-view { height: calc(100dvh - 115px); }
  .chips { overflow-x: auto; max-width: 40vw; }
  .w-hud { max-width: calc(100% - 32px); }
}
''')
