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
              circle), apple-touch-icon (180) and eg-mark(-ink).png cutouts for
              in-app decoration. All share the exact Android/iOS art, so the
              seam between platforms is zero.

Run from anywhere:  python3 web/scripts/make-icons.py   (requires Pillow)
Commit the outputs — CI does not run this (no toolchain in the golden path).
"""
from __future__ import annotations

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
    # white emblem: opacity = luminance; ink emblem: opacity = inverted luminance.
    # Both involve the *content* only, so the (uniform) background vanishes.
    src = lum if white else ImageOps.invert(lum)
    alpha = src.point(lambda v: 0 if v < THRESHOLD else v)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = alpha.resize((round(size * scale), round(size * scale)), Image.LANCZOS)
    color = Image.new("RGBA", d.size, (255, 255, 255, 255) if white else (0, 0, 0, 255))
    out.paste(color, ((size - d.width) // 2, (size - d.height) // 2), d)
    return out


def composited(size: int, bg: tuple[int, int, int], white: bool, scale: float) -> Image.Image:
    base = Image.new("RGB", (size, size), bg)
    em = emblem(size, white, scale)
    base.paste(em, mask=em.split()[3])
    return base


def save(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True)
    print(f"icons: {path.relative_to(REPO)} <- {img.size[0]}×{img.size[1]}")


def main() -> None:
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

    # ── Web
    web = REPO / "web/public"
    save(composited(32, (0, 0, 0), white=True, scale=WEB_SCALE), web / "favicon.png")
    for size in (192, 512):
        save(composited(size, (0, 0, 0), white=True, scale=WEB_SCALE),
             web / f"icons/icon-{size}.png")
    save(composited(512, (0, 0, 0), white=True, scale=MASKABLE_SCALE),
         web / "icons/maskable-512.png")
    save(composited(180, (0, 0, 0), white=True, scale=WEB_SCALE),
         web / "icons/apple-touch-icon.png")
    # In-app decoration cutouts (transparent): white for dark surfaces,
    # ink for light surfaces — used by the header wordmark + splash-ish bits.
    emblem(256, white=True, scale=0.98).save(web / "eg-mark.png", format="PNG", optimize=True)
    emblem(256, white=False, scale=0.98).save(web / "eg-mark-ink.png", format="PNG", optimize=True)
    print("icons: eg-mark.png / eg-mark-ink.png cutouts")


if __name__ == "__main__":
    main()
