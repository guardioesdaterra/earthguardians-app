#!/usr/bin/env bash
# Publish captured screenshots as real JPG files on the `ci-shots` branch so
# the run summary can embed them via direct raw.githubusercontent.com URLs.
#
# GitHub's Markdown sanitizer does not render data:/base64 image URIs (they
# show up as broken-image icons), while raw URLs from an allowed host render
# inline AND are true direct .jpg file URLs. Files are keyed by run number
# (runs/<n>/android|ios/*.jpg) so previous runs keep rendering too.
#
# Usage: publish_screenshots.sh <android|ios> [source-dir-with-images]
# Side effect: exports EG_SHOTS_BASE_URL into GITHUB_ENV for the summary step.
set -euo pipefail

name="${1:?usage: publish_screenshots.sh <android|ios> [dir]}"
dir="${2:-shots}"
branch="ci-shots"
repo="$GITHUB_REPOSITORY"
run="${GITHUB_RUN_NUMBER:?}"
work="${RUNNER_TEMP:-/tmp}/ci-shots-work"

# ── 1) Convert the captures to compact JPGs ─────────────────────────────────
jpg_dir="${RUNNER_TEMP:-/tmp}/shots-jpg"
rm -rf "$jpg_dir"
mkdir -p "$jpg_dir"

# Pillow is absent from the runners' base images — install it here so the
# publish step doesn't depend on the (later) summary-renderer install step.
if ! python3 -c "import PIL" >/dev/null 2>&1; then
  python3 -m pip install --user --quiet pillow \
    || python3 -m pip install --break-system-packages --user --quiet pillow
fi

# Conversion failure must not kill the job — exiting 0 without publishing
# makes the summary renderer fall back to data URIs instead.
python3 - "$dir" "$jpg_dir" <<'PY' \
  || echo "[publish] JPG conversion failed — summary will fall back to data URIs"
import glob, os, sys
from PIL import Image
src, dst = sys.argv[1], sys.argv[2]
paths = sorted(
    p for p in glob.glob(os.path.join(src, "*"))
    if p.lower().endswith((".png", ".jpg", ".jpeg"))
)
if not paths:
    print(f"[publish] no images to convert in {src!r}", file=sys.stderr)
for p in paths:
    img = Image.open(p).convert("RGB")
    max_w = 480
    if img.width > max_w:
        img = img.resize((max_w, int(img.height * max_w / img.width)), Image.LANCZOS)
    out = os.path.join(dst, os.path.splitext(os.path.basename(p))[0] + ".jpg")
    img.save(out, format="JPEG", quality=72, optimize=True)
    print(f"[publish] {os.path.basename(out)} ({os.path.getsize(out) // 1024} KiB)")
PY

# ── 2) Lay them onto the ci-shots branch, keyed by run number ───────────────
rm -rf "$work"
if git clone --depth 1 --branch "$branch" \
     "https://x-access-token:${GITHUB_TOKEN}@github.com/${repo}.git" "$work" 2>/dev/null; then
  echo "[publish] extending existing $branch branch"
else
  rm -rf "$work"
  git init -q -b "$branch" "$work"
  git -C "$work" remote add origin \
    "https://x-access-token:${GITHUB_TOKEN}@github.com/${repo}.git"
  echo "[publish] creating orphan $branch branch"
fi
dest="$work/runs/$run/$name"
rm -rf "$dest"
mkdir -p "$dest"
cp "$jpg_dir"/*.jpg "$dest"/ 2>/dev/null || { echo "[publish] nothing to publish"; exit 0; }

git -C "$work" add -A
if git -C "$work" -c user.name="eg-ci" -c user.email="dev@earthguardians.org" \
     commit -qm "screenshots: run $run ($name)"; then
  git -C "$work" push -q origin "$branch"
  echo "[publish] pushed to $branch"
else
  echo "[publish] nothing new to commit"
fi

base_url="https://raw.githubusercontent.com/${repo}/${branch}/runs/${run}/${name}"
echo "EG_SHOTS_BASE_URL=$base_url" >> "${GITHUB_ENV:?}"
echo "[publish] $base_url"
