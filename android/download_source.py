"""Download the exact public sdist used by the native Android wheel job."""
import json
import os
from pathlib import Path
import tarfile
import urllib.request

name, version = os.environ["PACKAGE"].split("==")
with urllib.request.urlopen(f"https://pypi.org/pypi/{name}/{version}/json") as response:
    metadata = json.load(response)
source = next(row for row in metadata["urls"] if row["packagetype"] == "sdist")
urllib.request.urlretrieve(source["url"], "source.tar.gz")
with tarfile.open("source.tar.gz") as archive:
    archive.extractall("unpacked", filter="data")
next(Path("unpacked").iterdir()).rename("source")
