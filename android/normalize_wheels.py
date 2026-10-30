"""Use CPython's portable .so suffix, shared by Chaquopy's Android interpreter."""
import base64
import csv
import hashlib
import io
import re
import zipfile
from pathlib import Path

for path in (Path(__file__).parent / "wheels").glob("*.whl"):
    with zipfile.ZipFile(path) as archive:
        files = {re.sub(r"\.cpython-\d+-[^/]+\.so$", ".so", name): archive.read(name)
                 for name in archive.namelist() if not name.endswith("/")}
    record = next(name for name in files if name.endswith(".dist-info/RECORD"))
    buffer = io.StringIO()
    writer = csv.writer(buffer, lineterminator="\n")
    for name, data in files.items():
        if name != record:
            digest = base64.urlsafe_b64encode(hashlib.sha256(data).digest()).rstrip(b"=").decode()
            writer.writerow([name, "sha256=" + digest, len(data)])
    writer.writerow([record, "", ""])
    files[record] = buffer.getvalue().encode()
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in files.items():
            archive.writestr(name, data)
    print("Normalized native extension names:", path.name)
