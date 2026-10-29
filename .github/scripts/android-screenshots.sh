#!/usr/bin/env bash
# Android screenshot capture — main screens of the app on the CI emulator.
# Strict mode for verification: the step only succeeds if the app is actually
# installed, foreground, navigated to each expected screen and showing the
# known per-screen text. On any failure we dump the app's verbose error output
# (crash buffer + error-level logcat + UI hierarchy) into the CI log and the
# artifact so diagnosis needs no local reproduction.
set -uo pipefail

PKG="org.earthguardians.app"
SHOTS="shots"
LOG="$SHOTS/app-log.txt"
mkdir -p "$SHOTS"

ADB="adb"

apk() {
  # Local gradle layout first, then the download-artifact layout (v4 keeps
  # the debug/ and release/ prefixes once an artifact has more than one APK).
  ls app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1 \
    || ls debug/*.apk 2>/dev/null | head -1 \
    || ls release/*.apk 2>/dev/null | head -1 \
    || ls *.apk 2>/dev/null | head -1
}

dump_app_log() { # reasons are printed for the CI log AND stored with the artifact
  local reason="$1"
  echo "[shots][ERROR] $reason" | tee -a "$LOG"
  echo "[shots][ERROR] — crash buffer:" | tee -a "$LOG"
  "$ADB" logcat -d -b crash 2>&1 | tail -120 | tee -a "$LOG" || true
  echo "[shots][ERROR] — error-level logcat (tail):" | tee -a "$LOG"
  "$ADB" logcat -d -v time "*:E" 2>&1 | tail -80 | tee -a "$LOG" || true
  # Our app's own error channel (navigation failures, sync errors) is easy to
  # miss inside the Google-suite noise — grep it out explicitly.
  echo "[shots][ERROR] — the app's own log lines (EgRoute/EgSync/AndroidRuntime):" | tee -a "$LOG"
  "$ADB" logcat -d -v time 2>&1 | grep -iE "EgRoute|EgSync|AndroidRuntime|org\.earthguardians" \
    | tail -40 | tee -a "$LOG" || echo "(no app log lines)" | tee -a "$LOG"
  echo "[shots][ERROR] — focused window / resumed activity:" | tee -a "$LOG"
  {
    "$ADB" shell dumpsys window 2>/dev/null | grep -i "mCurrentFocus\|mFocusedApp" || true
    "$ADB" shell dumpsys activity activities 2>/dev/null | grep -im1 "topResumedActivity" || true
  } | tee -a "$LOG"
  echo "[shots][ERROR] full table above; artifact also carries $LOG"
}

is_foreground() { # 0 if our activity is the resumed one
  local focus
  focus=$("$ADB" shell dumpsys activity activities 2>/dev/null | grep -i "topResumedActivity\|mResumedActivity" | head -2)
  echo "$focus" | grep -q "$PKG/.MainActivity" && return 0
  focus=$("$ADB" shell dumpsys window 2>/dev/null | grep -i "mCurrentFocus")
  echo "$focus" | grep -q "$PKG" && return 0
  echo "[shots][WARN] not foreground: ${focus:-<empty>}"
  return 1
}

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
attr_re = re.compile(r'(?:text|content-desc)="([^"]*)"')
for node in xml.split("<node"):
    # Substring match: screens carry whole sentences in one node
    # ("Every crew gathering, live from the site feed.") and the expectation
    # is a stable fragment of it. text is the visible string; content-desc is
    # the a11y label — both legitimate visibility signals.
    if any(text in v and v for v in attr_re.findall(node)):
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

wait_visible() { # $1 = expected text; verifies via uiautomator dump for up to 10s
  local text="$1" i
  for i in $(seq 10); do
    if [ -n "$(uia_bounds_for_text "$text" | head -1)" ]; then
      echo "[shots] verified: '$text' visible"
      return 0
    fi
    sleep 1
  done
  echo "[shots][ERROR] expected text '$text' NOT visible on screen"
  return 1
}

deeplink() { # $1 = uri — errors are loud (an uninstalled/broken app must be seen)
  echo "[shots] route → $1"
  $ADB shell am start -a android.intent.action.VIEW -d "$1" "$PKG"
}

# ── Install + first launch ───────────────────────────────────────────────────
APK_PATH="$(apk)"
if [ -z "$APK_PATH" ]; then
  echo "[shots][ERROR] no APK found anywhere in the workspace:"
  find . -maxdepth 6 -name "*.apk" 2>/dev/null | head
  echo "[shots][ERROR] the download-artifact step dropped it outside app/build/outputs"
  exit 1
fi
echo "[shots] installing $APK_PATH"
"$ADB" install -r -t "$APK_PATH" || { dump_app_log "adb install failed"; exit 1; }
"$ADB" shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS > /dev/null 2>&1 || true

# Calm animations so captures are steady.
"$ADB" shell settings put global window_animation_scale 0 > /dev/null
"$ADB" shell settings put global transition_animation_scale 0 > /dev/null
"$ADB" shell settings put global animator_duration_scale 0 > /dev/null

"$ADB" shell am start -n "$PKG/.MainActivity" || { dump_app_log "am start failed"; exit 1; }

# Cold-start crash guard: require the MAIN ACTIVITY foreground within 30s —
# a blank/launcher screen or a startup crash is a hard failure with output.
foreground_ok=""
for _ in $(seq 10); do
  if is_foreground; then foreground_ok=1; break; fi
  "$ADB" shell am start -n "$PKG/.MainActivity" > /dev/null 2>&1 || true
  sleep 3
done
[ -n "$foreground_ok" ] || { dump_app_log "app never became the foreground activity (blank screen or crash)"; exit 1; }
sleep 3

# ── Complete onboarding (Skip → onFinished) ──────────────────────────────────
tap_text "Skip" 10 || tap_text "Get started" 4
sleep 2

# ── Screen with expectations: name | route | text the screen must show ──────
declare -a SCREENS=(
  "01-home|earthguardians://home|Upcoming"
  "02-events|earthguardians://events|Every crew gathering"
  "03-stories|earthguardians://stories|Shared on the blog"
  "04-maps|earthguardians://maps|EG-Maps"
  "05-more|earthguardians://more|More"
  "06-section-donate|earthguardians://section?u=https%3A%2F%2Fwww.earthguardians.org%2Fdonate|Donate"
)

FAILURES=0
failed_names=""
for entry in "${SCREENS[@]}"; do
  IFS='|' read -r name route expected <<< "$entry"
  deeplink "$route" || { dump_app_log "deeplink am start failed for $route"; exit 1; }
  if ! is_foreground; then
    sleep 4
    if ! is_foreground; then
      dump_app_log "app lost foreground while opening $route (crash?)"
      exit 1
    fi
  fi
  if [ -n "$expected" ]; then
    if wait_visible "$expected"; then
      # Root pages pay an extra settle so live content (feed, WebView page)
      # is on screen when the frame is taken.
      sleep 5
      shot "$name"
    else
      dump_app_log "screen verification failed for $name (expected '$expected')"
      failed_names="$failed_names $name"
      FAILURES=$((FAILURES + 1))
    fi
  else
    # WebView route: no stable ui-text contract — foreground only.
    sleep 3
    shot "$name"
  fi
  sleep 2
done

if [ "$FAILURES" -gt 0 ]; then
  echo "[shots][ERROR] failed screens:$failed_names"
  exit 1
fi

echo "[shots] done: $(ls "$SHOTS" | wc -l) captures, all screens verified"
exit 0
