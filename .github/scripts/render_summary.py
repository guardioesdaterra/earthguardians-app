#!/usr/bin/env python3
"""
Render captured screenshots into the GitHub Actions run summary (GITHUB_STEP_SUMMARY).

Preferred mode (--base-url): reference the published direct JPG URLs
(raw.githubusercontent.com files on the ci-shots branch) — GitHub's Markdown
sanitizer does not render data:/base64 URIs, they show as broken images.

Fallback: downscaled JPEG-compressed base64 data URIs. Non-fatal: if Pillow is
missing or the summary file is absent it degrades to a plain file list.

Usage: render_summary.py "<section title>" <dir-with-images>
              [--base-url https://raw.githubusercontent.com/…/<name>] [--max-width N]
"""
import base64
import io
import os
import sys

try:
    from PIL import Image
    HAS_PIL = True
except ImportError:
    HAS_PIL = False


def as_data_uri(path: str, max_width: int) -> str | None:
    """Downscale → JPEG → base64 data URI (or None if Pillow is unavailable)."""
    with Image.open(path) as img:
        img = img.convert("RGB")
        if img.width > max_width:
            height = int(img.height * max_width / img.width)
            img = img.resize((max_width, height), Image.LANCZOS)
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=68, optimize=True)
        return "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()


def main() -> int:
    title = sys.argv[1] if len(sys.argv) > 1 else "Screenshots"
    folder = sys.argv[2] if len(sys.argv) > 2 else "shots"
    max_width = 420
    if "--max-width" in sys.argv:
        max_width = int(sys.argv[sys.argv.index("--max-width") + 1])
    base_url = None
    if "--base-url" in sys.argv:
        base_url = sys.argv[sys.argv.index("--base-url") + 1] or None

    names = sorted(n for n in os.listdir(folder) if n.lower().endswith((".png", ".jpg", ".jpeg")))
    if not names:
        print(f"[render_summary] no images in {folder!r}", file=sys.stderr)
        return 0

    lines = [f"## 📸 {title}", ""]
    for name in names:
        label = os.path.splitext(name)[0].replace("-", " ").replace("_", " ").strip().title()
        path = os.path.join(folder, name)
        if base_url:
            # True direct JPG URLs — render inline on the run page and open as
            # plain .jpg files; basename must match what publish_screenshots.sh
            # wrote on the ci-shots branch (same stem, .jpg extension).
            stem = os.path.splitext(name)[0]
            lines.append(f'<p align="center"><strong>{label}</strong><br>'
                         f'<a href="{base_url}/{stem}.jpg"><img width="{max_width}" '
                         f'src="{base_url}/{stem}.jpg" alt="{label}"></a></p>')
            continue
        uri = as_data_uri(path, max_width) if HAS_PIL else None
        if uri:
            lines.append(f'<p align="center"><strong>{label}</strong><br>'
                         f'<img width="{max_width}" src="{uri}" alt="{label}"></p>')
        else:
            lines.append(f"- `{name}` (artifact)")
    lines.append("")

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary:
        print("[render_summary] GITHUB_STEP_SUMMARY not set; skipped", file=sys.stderr)
        return 0
    with open(summary, "a", encoding="utf-8") as fh:
        fh.write("\n".join(lines))
    print(f"[render_summary] embedded {len(names)} screenshots into summary")
    return 0


if __name__ == "__main__":
    sys.exit(main())
