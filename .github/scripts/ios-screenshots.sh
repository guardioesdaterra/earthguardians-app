#!/usr/bin/env bash
# iOS screenshot capture — main screens on a CI simulator.
# Assumes the Xcode project has already been generated (XcodeGen).
# Every failure is tolerated (CI job is continue-on-error).
set -uo pipefail

cd "$(dirname "$0")/../.." # repo root

PKG="org.earthguardians.app"
SHOTS="shots"
SIM_APP="ios/build/sim/Build/Products/Debug-iphonesimulator/EarthGuardians.app"
mkdir -p "$SHOTS"

echo "[shots] picking an available iPhone simulator…"
UDID=$(xcrun simctl list devices available \
  | grep "iPhone" \
  | grep -oE "[A-F0-9]{8}-([A-F0-9]{4}-){3}[A-F0-9]{12}" | head -1)
if [ -z "$UDID" ]; then
  echo "[shots] no simulator available"; exit 0
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
  -quiet build || { echo "[shots] build failed"; exit 0; }

xcrun simctl boot "$UDID" 2>/dev/null || true
xcrun simctl bootstatus "$UDID" -b >/dev/null

xcrun simctl install "$UDID" "$SIM_APP" || { echo "[shots] install failed"; exit 0; }

# Launch with an NSArgumentDomain override that marks onboarding complete —
# the app reads UserDefaults for "onboarding_complete" at startup, so the
# gate is skipped without any UI automation.
xcrun simctl launch "$UDID" "$PKG" -onboarding_complete 1 >/dev/null
sleep 8

shot() { # $1 = name
  xcrun simctl io "$UDID" screenshot "$SHOTS/$1.png" >/dev/null 2>&1 \
    && echo "[shots] captured $1.png" \
    || echo "[shots] screenshot failed for $1"
}

deeplink() { # $1 = uri
  echo "[shots] route → $1"
  xcrun simctl openurl "$UDID" "$1" >/dev/null 2>&1 || echo "[shots] openurl failed"
  sleep 4
}

deeplink "earthguardians://home";    shot 01-home
deeplink "earthguardians://events";  shot 02-events
deeplink "earthguardians://stories"; shot 03-stories
deeplink "earthguardians://maps";    shot 04-maps
deeplink "earthguardians://more";    shot 05-more
deeplink "earthguardians://section?u=https%3A%2F%2Fwww.earthguardians.org%2Fdonate"; shot 06-section-donate

xcrun simctl terminate "$UDID" "$PKG" >/dev/null 2>&1 || true
xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true
echo "[shots] done: $(ls "$SHOTS" 2>/dev/null | wc -l) captures"
exit 0
