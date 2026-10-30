"""Give PyO3 the target interpreter config, not the host crossenv executable."""
import os
import subprocess
import sysconfig
from pathlib import Path

target = os.environ["CARGO_BUILD_TARGET"]
subprocess.run(["rustup", "target", "add", target], check=True)
libdir = Path(sysconfig.get_config_var("LIBDIR"))
if not (libdir / "libpython3.13.so").exists():
    candidates = list((Path.home() / ".cache/cibuildwheel").rglob("libpython3.13.so"))
    if not candidates:
        raise RuntimeError(f"Target libpython missing: {libdir}")
    libdir = next((p.parent for p in candidates if ("aarch64" in str(p)) == target.startswith("aarch64")), candidates[0].parent)
config = "\n".join([
    "implementation=CPython", "version=3.13", "shared=true", "abi3=false",
    "lib_name=python3.13", f"lib_dir={libdir}", "pointer_width=64",
])
Path("/tmp/nathan-pyo3.txt").write_text(config + "\n")
Path(".cargo").mkdir(exist_ok=True)
Path(".cargo/config.toml").write_text(f'[target.{target}]\nlinker = "{os.environ["CC"]}"\n')
print("Target Python:", libdir, "linker:", os.environ["CC"])
