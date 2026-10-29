#!/usr/bin/env python3
"""Compare the PWA Home capture with the content-only Android reference crop."""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REFERENCE = ROOT / "tests/fixtures/pwa-home/android-home-run-40876025810.png"
PWA_CAPTURE = ROOT / "artifacts/pwa-home.png"
MASKS_FILE = ROOT / "artifacts/pwa-home-masks.json"
ARTIFACTS = ROOT / "artifacts"

# The Android screencap is 1080x2400. Pixel 6 status/camera content ends at
# row 149; the bottom gesture-navigation inset starts at row 2336.
NATIVE_CONTENT_CROP = (0, 149, 1080, 2336)
EXPECTED_SIZE = (1080, 2187)
CHANNEL_THRESHOLD = 48
MAX_CHANGED_PIXEL_RATIO = 0.12
MAX_MEAN_ABSOLUTE_ERROR = 12.0
MAX_MASKED_RATIO = 0.30


def fail(message: str) -> "NoReturn":
    print(f"[pwa-home-visual][ERROR] {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> int:
    for path in (REFERENCE, PWA_CAPTURE, MASKS_FILE):
        if not path.is_file():
            fail(f"Required input is missing: {path}")

    reference = Image.open(REFERENCE).convert("RGB")
    if reference.size != (1080, 2400):
        fail(f"Android reference must be 1080x2400, got {reference.size}")
    native = reference.crop(NATIVE_CONTENT_CROP)
    pwa = Image.open(PWA_CAPTURE).convert("RGB")
    if native.size != EXPECTED_SIZE:
        fail(f"Native content crop has unexpected dimensions: {native.size}")
    if pwa.size != EXPECTED_SIZE:
        fail(f"PWA screenshot must be {EXPECTED_SIZE[0]}x{EXPECTED_SIZE[1]}, got {pwa.size}")

    mask_data = json.loads(MASKS_FILE.read_text(encoding="utf-8"))
    if (mask_data.get("width"), mask_data.get("height")) != EXPECTED_SIZE:
        fail("Mask file dimensions do not match the content crop")
    masks = mask_data.get("masks")
    if not isinstance(masks, list):
        fail("Media masks must be a list")

    masked = np.zeros((EXPECTED_SIZE[1], EXPECTED_SIZE[0]), dtype=bool)
    for index, item in enumerate(masks):
        try:
            x, y, width, height = (int(item[key]) for key in ("x", "y", "width", "height"))
        except (KeyError, TypeError, ValueError):
            fail(f"Mask #{index} has invalid geometry")
        if width <= 0 or height <= 0 or x < 0 or y < 0 or x + width > EXPECTED_SIZE[0] or y + height > EXPECTED_SIZE[1]:
            fail(f"Mask #{index} is outside the compared content crop: {item}")
        masked[y : y + height, x : x + width] = True

    masked_ratio = float(masked.mean())
    if masked_ratio > MAX_MASKED_RATIO:
        fail(f"Masks cover {masked_ratio:.1%} of the content (limit {MAX_MASKED_RATIO:.0%}); narrow the media selectors")
    valid = ~masked
    if not valid.any():
        fail("The masks leave no pixels to compare")

    expected = np.asarray(native, dtype=np.int16)
    actual = np.asarray(pwa, dtype=np.int16)
    delta = np.abs(actual - expected)
    mae = float(delta[valid].mean())
    changed_ratio = float((delta.max(axis=2)[valid] > CHANNEL_THRESHOLD).mean())

    # Keep inspectable CI artifacts without making a visual-parity claim.
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    native.save(ARTIFACTS / "android-home-content-crop.png")
    heat = np.clip(delta.max(axis=2).astype(np.float32) * 3.0, 0, 255).astype(np.uint8)
    diff_rgb = np.stack((heat, heat // 7, heat // 12), axis=2)
    diff_rgb[masked] = (128, 128, 128)
    Image.fromarray(diff_rgb, mode="RGB").save(ARTIFACTS / "pwa-home-diff.png")

    passed = mae <= MAX_MEAN_ABSOLUTE_ERROR and changed_ratio <= MAX_CHANGED_PIXEL_RATIO
    status = "PASS" if passed else "FAIL"
    lines = [
        f"### PWA Home visual comparison: {status}",
        "",
        f"- Android content crop: `{NATIVE_CONTENT_CROP}` → `{EXPECTED_SIZE[0]}×{EXPECTED_SIZE[1]}` px.",
        f"- Thresholds: MAE ≤ `{MAX_MEAN_ABSOLUTE_ERROR:.1f}`; changed pixels (max RGB delta > `{CHANNEL_THRESHOLD}`) ≤ `{MAX_CHANGED_PIXEL_RATIO:.0%}`.",
        f"- Result: MAE `{mae:.2f}`; changed pixels `{changed_ratio:.2%}`; masked media `{masked_ratio:.2%}` ({len(masks)} rectangles).",
        "- Only nondeterministic remote media is masked; bundled assets and intentional placeholders remain compared.",
        "- This is a masked screenshot regression signal, not a claim of complete pixel parity.",
    ]
    summary = "\n".join(lines) + "\n"
    print(summary)
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as output:
            output.write(summary)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
