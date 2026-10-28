#!/usr/bin/env bash
# Android screenshot capture — main screens of the app on the CI emulator.
# Runs INSIDE reactivecircus/android-emulator-runner (device already booted).
# Every failure here is tolerated (the CI job is continue-on-error) — we still
# capture as much as we can and always produce the artifact + summary.
set -uo pipefail

PKG="org.earthguardians.app"
SHOTS="shots"
mkdir -p "$SHOTS"

ADB="adb"

apk() {
  ls app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1 \
    || ls app/build/outputs/apk/release/*.apk 2>/dev/null | head -1
}

# ── UI helpers ────────────────────────────────────────────────────────────────
uia_bounds_for_text() { # $1 = visible text → "cx cy" or empty
  local text="$1" dump="/sdcard/eg_dump.xml"
  "$ADB" shell uiautomator dump /sdcard/eg_dump.xml >/dev/null 2>&1
  "$ADB" pull "$dump" /tmp/eg_dump.xml >/dev/null 2>&1
  python3 - "$text" <<'PY'
import re, sys
text = sys.argv[1]
try:
    xml = open("/tmp/eg_dump.xml", encoding="utf-8", errors="ignore").read()
except OSError:
    sys.exit(0)
for node in xml.split("<node"):
    if f'text="{text}"' in node:
        m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if m:
            x1, y1, x2, y2 = map(int, m.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
}

tap_text() { # $1 = text, $2 = max attempts
  local text="$1" tries="${2:-6}" xy
  for _ in $(seq "$tries"); do
    xy=$(uia_bounds_for_text "$text")
    if [ -n "$xy" ]; then
      echo "[shots] tapping '$text' at $xy"
      $ADB shell input tap $xy
      return 0
    fi
    sleep 1.5
  done
  echo "[shots] '$text' not found — continuing"
  return 1
}

shot() { # $1 = name
  "$ADB" exec-out screencap -p > "$SHOTS/$1.png" 2>/dev/null \
    && echo "[shots] captured $1.png" \
    || echo "[shots] screencap failed for $1"
}

deeplink() { # $1 = uri
  echo "[shots] route → $1"
  $ADB shell am start -a android.intent.action.VIEW -d "$1" "$PKG" >/dev/null 2>&1
  sleep 4
}

# ── Install + first launch ───────────────────────────────────────────────────
APK_PATH="$(apk)"
echo "[shots] installing $(basename "$APK_PATH")"
$ADB install -r -t "$APK_PATH" >/dev/null
$ADB shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true

# Calm animations so captures are steady.
$ADB shell settings put global window_animation_scale 0 >/dev/null
$ADB shell settings put global transition_animation_scale 0 >/dev/null
$ADB shell settings put global animator_duration_scale 0 >/dev/null

$ADB shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 6

# ── Complete onboarding (Skip → onFinished) ──────────────────────────────────
tap_text "Skip" 10 || tap_text "Get started" 4
sleep 4

# ── Main screens ─────────────────────────────────────────────────────────────
deeplink "earthguardians://home";    shot 01-home
deeplink "earthguardians://events";  shot 02-events
deeplink "earthguardians://stories"; shot 03-stories
deeplink "earthguardians://maps";    shot 04-maps
deeplink "earthguardians://more";    shot 05-more
# Sitemap-driven themed browser (donate page).
deeplink "earthguardians://section?u=https%3A%2F%2Fwww.earthguardians.org%2Fdonate"; shot 06-section-donate

echo "[shots] done: $(ls "$SHOTS" | wc -l) captures"
exit 0
