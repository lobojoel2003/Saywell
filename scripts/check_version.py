#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Check that version.properties, CHANGELOG.md and (optionally) a release tag agree.

    python3 scripts/check_version.py           # version is valid and has a changelog section
    python3 scripts/check_version.py v0.1.0    # ...and matches this tag
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def version() -> str:
    for line in (ROOT / "version.properties").read_text().splitlines():
        if line.startswith("version="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("version.properties has no version= line")


def version_code(v: str) -> int:
    major, minor, patch = (int(p) for p in v.split("."))
    return major * 10000 + minor * 100 + patch


def main() -> None:
    v = version()
    if not re.fullmatch(r"\d+\.\d{1,2}\.\d{1,2}", v):
        raise SystemExit(f"version.properties: '{v}' must be MAJOR.MINOR.PATCH with MINOR and PATCH below 100")
    changelog = (ROOT / "CHANGELOG.md").read_text()
    if not re.search(rf"^## \[{re.escape(v)}\]", changelog, re.M):
        raise SystemExit(f"CHANGELOG.md needs a '## [{v}]' section")
    if len(sys.argv) > 1:
        tag = sys.argv[1].removeprefix("refs/tags/")
        if tag != f"v{v}":
            raise SystemExit(f"Tag {tag} does not match version.properties (v{v})")
    print(f"Saywell {v}, build {version_code(v)}")


if __name__ == "__main__":
    main()
