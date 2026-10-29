#!/usr/bin/env bash
# iOS screenshot capture — main screens on a CI simulator.
# Assumes the Xcode project has already been generated (XcodeGen).
# Strict mode for verification: after building and installing, the app is
# relaunched per screen via a launch argument (-eg_launch_route), which avoids
# the Safari 'Open in Earth Guardians' confirmation sheet that
# `simctl openurl` with custom schemes parks on top of every capture, and
# guarantees each screenshot is the screen it claims to be. The app's
# liveness is checked per route; crashes dump verbose app logs into the CI
# log and the artifact.
set -uo pipefail

cd "$(dirname "$0")/../.." # repo root

PKG="org.earthguardians.app"
SHOTS="shots"
LOG="$SHOTS/app-log.txt"
SIM_APP="ios/build/sim/Build/Products/Debug-iphonesimulator/EarthGuardians.app"
mkdir -p "$SHOTS"

dump_app_log() { # placed in CI log + artifact; mirrors the Android contract
  local reason="$1"
  echo "[shots][ERROR] $reason" | tee -a "$LOG"
  # Unfiltered recent log for OUR process — grep filters once blinded this dump.
  echo "[shots][ERROR] — simulator log for the app process (recent):" | tee -a "$LOG"
  xcrun simctl spawn "$UDID" log show --style compact --last 3m \
    --predicate 'process == "EarthGuardians"' 2>&1 \
    | grep -viE "AudioSession|MediaRemote|network connection|CoreText" \
    | tail -100 | tee -a "$LOG" || true
  # Crash reports land on the HOST for simulator apps — copy any fresh ones.
  echo "[shots][ERROR] — fresh DiagnosticReports (if any):" | tee -a "$LOG"
  find ~/Library/Logs/DiagnosticReports -mmin -5 -name "EarthGuardians*" -print -exec sh -c 'echo "== $1"; head -60 "$1"' _ {} \; 2>/dev/null | tee -a "$LOG" || true
}

alive() { # 0 if the app currently has a running process on the simulator
  # Simulator app processes are children of the host launchd: pgrep on the
  # install path is the reliable oracle. launchctl list is a best-effort
  # secondary (it lists UIKitApplication entries inconsistently).
  pgrep -f "$SIM_APP/EarthGuardians" >/dev/null 2>&1 && return 0
  xcrun simctl spawn "$UDID" launchctl list 2>/dev/null | grep -q "UIKitApplication:$PKG" \
    || xcrun simctl spawn "$UDID" launchctl list 2>/dev/null | grep -q "$PKG"
}

echo "[shots] picking an available iPhone simulator…"
UDID=$(xcrun simctl list devices available \
  | grep "iPhone" \
  | grep -oE "[A-F0-9]{8}-([A-F0-9]{4}-){3}[A-F0-9]{12}" | head -1)
if [ -z "$UDID" ]; then
  echo "[shots] no simulator available"; exit 1
fi
DEVICE_NAME=$(xcrun simctl list devices | grep "$UDID" | sed 's/.*(//' | tr -d ' )')
echo "[shots] simulator: $DEVICE_NAME ($UDID)"

echo "[shots] building Debug for simulator…"
xcodebuild \
  -project ios/EarthGuardians.xcodeproj \
  -scheme EarthGuardians \
  -configuration Debug \
  -destination "id=$UDID" \
  -derivedDataPath ios/build/sim \
  -quiet build || { echo "[shots][ERROR] xcodebuild failed"; exit 1; }

xcrun simctl boot "$UDID" 2>/dev/null || true
xcrun simctl bootstatus "$UDID" -b >/dev/null

xcrun simctl install "$UDID" "$SIM_APP" || { echo "[shots][ERROR] install failed"; exit 1; }

shot() { # $1 = name
  xcrun simctl io "$UDID" screenshot "$SHOTS/$1.png" >/dev/null 2>&1 \
    && echo "[shots] captured $1.png" \
    || echo "[shots][ERROR] screenshot failed for $1"
}

# Screens: name|launch route. Each screen is a fresh cold launch with the
# route argument — deterministic, no dialogs, no leftover state.
declare -a SCREENS=(
  "01-home|home"
  "02-events|events"
  "03-stories|stories"
  "04-maps|maps"
  "05-more|more"
  "06-section-nested-query|section?u=https://www.earthguardians.org/donate?source=ci&campaign=deeplink#ci-anchor"
)

for entry in "${SCREENS[@]}"; do
  IFS='|' read -r name route <<< "$entry"
  echo "[shots] screen $name ← route $route (cold launch)"
  xcrun simctl terminate "$UDID" "$PKG" >/dev/null 2>&1 || true
  xcrun simctl launch "$UDID" "$PKG" \
    -onboarding_complete 1 -eg_launch_route "$route" \
    || { dump_app_log "app failed to launch for $name (route $route)"; exit 1; }
  sleep 7
  if ! alive; then
    dump_app_log "app process died after launch (screen $name, route $route)"
    exit 1
  fi
  shot "$name"
  sleep 1
done

xcrun simctl terminate "$UDID" "$PKG" >/dev/null 2>&1 || true
xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true

echo "[shots] done: $(ls "$SHOTS" 2>/dev/null | wc -l) captures"
exit 0
