#!/usr/bin/env python3
"""
make-icons.py — regenerate application icons for ALL platforms (Android, iOS,
web) from the official Earth Guardians founder mark (the eye + EG seal).

Source of truth: the iOS AppIcon master `on-dark` variant (highest resolution:
1024×1024, white emblem on black). Everything else is derived:

  Android  →  adaptive-icon foreground + monochrome drawables (all 5 densities),
              transparent with the white emblem alpha-only (as before), but the
              emblem is now sized to the official adaptive safe zone
              (66/108 ≈ 61% of the canvas) instead of full-bleed, so masks stop
              cropping the rays.
  iOS      →  icon-light.png (ink on white) + icon-dark.png (white on ink/black),
              full 1024, emblem at 62%.
  Web      →  favicon.png, icon-192/512 ("any"), maskable-512 (55%, PWA safe
              circle), apple-touch-icon (180) and centered eg-mark(-ink).png
              cutouts for the compact in-app header. All share one cropped,
              proportion-preserving emblem and palette across platforms.

Run from anywhere:  python3 web/scripts/make-icons.py [--web-only] (Pillow).
The Pages workflow runs the web-only mode remotely; native builds stay CI-only.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    from PIL import Image, ImageOps
except ImportError:  # keep the error message actionable
    sys.exit("make-icons.py requires Pillow:  pip install pillow")

REPO = Path(__file__).resolve().parents[2]
MASTER = REPO / "ios/EarthGuardians/Assets.xcassets/AppIcon.appiconset/icon-dark.png"

ANDROID_DENSITIES = {  # adaptive-icon canvas sizes per density (108dp base)
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}

# Emblem size per canvas (fraction of the side). The old full-bleed artwork was
# cropped by circular/squircle masks — these sit inside every platform's safe
# zone while staying comfortably larger than minimums.
ANDROID_SCALE = 0.61
IOS_SCALE = 0.62
WEB_SCALE = 0.60
MASKABLE_SCALE = 0.55

THRESHOLD = 8  # luminance delta treated as ink (protects against gray noise)


def load_master() -> Image.Image:
    im = Image.open(MASTER).convert("RGB")
    if im.size != (1024, 1024):
        raise SystemExit(f"master must be 1024×1024, got {im.size}")
    return im


def emblem(size: int, white: bool, scale: float) -> Image.Image:
    """White (or black) emblem with real alpha, `scale` of `size`, centered."""
    master = load_master()
    lum = ImageOps.grayscale(master)
    # The official master is a white emblem on a dark tile. Its luminance is
    # therefore the emblem mask for both output colors; only the fill changes.
    alpha = lum.point(lambda v: 0 if v < THRESHOLD else v)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bounds = alpha.getbbox()
    if bounds is None:
        return out
    # The master is an app-icon tile, not a logo crop: trim its flat background
    # first so the requested scale refers to the actual emblem, not empty space.
    alpha = alpha.crop(bounds)
    factor = min((size * scale) / alpha.width, (size * scale) / alpha.height)
    d = alpha.resize((max(1, round(alpha.width * factor)), max(1, round(alpha.height * factor))), Image.LANCZOS)
    color = Image.new("RGBA", d.size, (255, 255, 255, 255) if white else (0, 0, 0, 255))
    out.paste(color, ((size - d.width) // 2, (size - d.height) // 2), d)
    return out


def header_mark(size: int, white: bool) -> Image.Image:
    """Crop the approved app-icon emblem into a transparent, header-sized mark."""
    source_path = MASTER
    master = Image.open(source_path).convert("RGB")
    lum = ImageOps.grayscale(master)
    alpha = lum.point(lambda v: 0 if v < THRESHOLD else v)
    bounds = alpha.getbbox()
    if bounds is None:
        raise SystemExit(f"no logo pixels found in {source_path}")

    alpha = alpha.crop(bounds)
    target_side = round(size * 0.9)
    scale = target_side / max(alpha.size)
    width = max(1, round(alpha.width * scale))
    height = max(1, round(alpha.height * scale))
    alpha = alpha.resize((width, height), Image.LANCZOS)

    color = Image.new("RGBA", alpha.size, (255, 255, 255, 255) if white else (0, 0, 0, 255))
    color.putalpha(alpha)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.alpha_composite(color, ((size - width) // 2, (size - height) // 2))
    return out


def write_header_assets() -> None:
    """Bundle the same official logo variants for Android and the iOS asset catalog."""
    android_dir = REPO / "app/src/main/res/drawable-nodpi"
    ios_dir = REPO / "ios/EarthGuardians/Assets.xcassets/EGBrandMark.imageset"
    variants = [
        ("on_light", "on-light", False),
        ("on_dark", "on-dark", True),
    ]

    ios_dir.mkdir(parents=True, exist_ok=True)
    for android_name, ios_name, white in variants:
        image = header_mark(256, white=white)
        save(image, android_dir / f"eg_brand_mark_{android_name}.png")
        save(image, ios_dir / f"eg-brand-mark-{ios_name}.png")

    contents = {
        "images": [
            {"filename": "eg-brand-mark-on-light.png", "idiom": "universal", "scale": "1x"},
            {
                "appearances": [{"appearance": "luminosity", "value": "dark"}],
                "filename": "eg-brand-mark-on-dark.png",
                "idiom": "universal",
                "scale": "1x",
            },
        ],
        "info": {"author": "xcode", "version": 1},
    }
    (ios_dir / "Contents.json").write_text(json.dumps(contents, indent=2) + "\n", encoding="utf-8")


def composited(size: int, bg: tuple[int, int, int], white: bool, scale: float) -> Image.Image:
    base = Image.new("RGB", (size, size), bg)
    em = emblem(size, white, scale)
    base.paste(em, mask=em.split()[3])
    return base


def save(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True)
    print(f"icons: {path.relative_to(REPO)} <- {img.size[0]}×{img.size[1]}")


def generate_web() -> None:
    web = REPO / "web/public"
    save(composited(32, (0, 0, 0), white=True, scale=WEB_SCALE), web / "favicon.png")
    for size in (192, 512):
        save(composited(size, (0, 0, 0), white=True, scale=WEB_SCALE),
             web / f"icons/icon-{size}.png")
    save(composited(512, (0, 0, 0), white=True, scale=MASKABLE_SCALE),
         web / "icons/maskable-512.png")
    save(composited(180, (0, 0, 0), white=True, scale=WEB_SCALE),
         web / "icons/apple-touch-icon.png")
    # The in-app mark has generous transparent padding: it reads smaller and
    # remains optically centered in the compact shared header across screens.
    save(emblem(256, white=True, scale=0.72), web / "eg-mark.png")
    save(emblem(256, white=False, scale=0.72), web / "eg-mark-ink.png")
    print("icons: official web mark is centered at 72% canvas scale")


def main() -> None:
    if "--web-only" in sys.argv:
        generate_web()
        return

    # ── iOS: light (ink on white) + dark (white on black), same frame as before
    save(composited(1024, (255, 255, 255), white=False, scale=IOS_SCALE),
         REPO / "ios/EarthGuardians/Assets.xcassets/AppIcon.appiconset/icon-light.png")
    save(composited(1024, (0, 0, 0), white=True, scale=IOS_SCALE),
         REPO / "ios/EarthGuardians/Assets.xcassets/AppIcon.appiconset/icon-dark.png")

    # ── Android: adaptive foreground + monochrome (alpha-only emblem, transparent)
    for dpi, size in ANDROID_DENSITIES.items():
        fg = emblem(size, white=True, scale=ANDROID_SCALE)
        save(fg, REPO / f"app/src/main/res/drawable-{dpi}/ic_launcher_foreground.png")
        save(fg, REPO / f"app/src/main/res/drawable-{dpi}/ic_launcher_monochrome.png")

    generate_web()
    write_header_assets()


if __name__ == "__main__":
    main()
