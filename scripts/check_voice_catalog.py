#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Check that every voice in the app's catalog can be downloaded, and print its size.

    python3 scripts/check_voice_catalog.py
"""

from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

CATALOG = Path(__file__).resolve().parent.parent / "app/src/main/assets/voices/catalog.json"


def check(url: str) -> tuple[str, int | None, str | None]:
    request = urllib.request.Request(url, method="HEAD", headers={"User-Agent": "saywell-ci"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return url, int(response.headers.get("Content-Length", 0)), None
    except urllib.error.HTTPError as e:
        return url, None, f"HTTP {e.code}"
    except OSError as e:
        return url, None, str(e)


def main() -> None:
    catalog = json.loads(CATALOG.read_text())
    urls = [f"{catalog['baseUrl']}vits-piper-{v['id']}.tar.bz2" for v in catalog["voices"]]
    failed = 0
    with ThreadPoolExecutor(max_workers=8) as pool:
        for url, size, error in pool.map(check, urls):
            name = url.rsplit("/", 1)[-1]
            if error:
                failed += 1
                print(f"::error::{name}: {error}")
            else:
                print(f"ok  {name}  {size / 1_048_576:.0f} MB")
    if failed:
        sys.exit(f"{failed} of {len(urls)} voices can't be downloaded")


if __name__ == "__main__":
    main()
