"""Give PyO3 the target interpreter config, not the host crossenv executable."""
import os
import subprocess
import sysconfig
from pathlib import Path

target = os.environ["CARGO_BUILD_TARGET"]
subprocess.run(["rustup", "target", "add", target], check=True)
libdir = Path(sysconfig.get_config_var("LIBDIR"))
if not (libdir / "libpython3.13.so").exists():
    candidates = [p for p in Path("/tmp").rglob("libpython3.13.so")
                  if "/python/prefix/lib/" in str(p)]
    if not candidates:
        raise RuntimeError(f"Target libpython missing: {libdir}")
    libdir = candidates[0].parent
config = "\n".join([
    "implementation=CPython", "version=3.13", "shared=true", "abi3=false",
    "lib_name=python3.13", f"lib_dir={libdir}", "pointer_width=64",
])
Path("/tmp/nathan-pyo3.txt").write_text(config + "\n")
sdk = Path(os.environ["ANDROID_HOME"])
ndk = max((sdk / "ndk").iterdir(), key=lambda p: tuple(int(v) for v in p.name.split(".")))
linker = ndk / "toolchains/llvm/prebuilt/linux-x86_64/bin" / f"{target}24-clang"
Path(".cargo").mkdir(exist_ok=True)
Path(".cargo/config.toml").write_text(f'[target.{target}]\nlinker = "{linker}"\n')
print("Target Python:", libdir, "linker:", linker)
