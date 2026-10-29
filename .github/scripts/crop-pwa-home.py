#!/usr/bin/env python3
"""Crop a device-scale viewport screenshot to the exact PWA comparison size."""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image


def main() -> int:
    if len(sys.argv) != 5:
        raise SystemExit("usage: crop-pwa-home.py INPUT OUTPUT WIDTH HEIGHT")

    source_path, output_path = Path(sys.argv[1]), Path(sys.argv[2])
    width, height = int(sys.argv[3]), int(sys.argv[4])
    with Image.open(source_path) as source:
        if source.width < width or source.height < height:
            raise SystemExit(
                f"Captured viewport is too small: got {source.size}, need at least {(width, height)}"
            )
        source.crop((0, 0, width, height)).save(output_path, format="PNG")

    print(f"Normalized PWA screenshot to {width}x{height}px at the top-left viewport origin")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
