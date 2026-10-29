#!/usr/bin/env python3
"""Check shared Android/iOS route and onboarding imagery contracts."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]
MATRIX_PATH = ROOT / "tests/ui-parity/screen-matrix.json"
ANDROID_PATH = ROOT / ".github/scripts/android-screenshots.sh"
IOS_PATH = ROOT / ".github/scripts/ios-screenshots.sh"
ANDROID_ONBOARDING = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/root/OnboardingScreen.kt"
IOS_ONBOARDING = ROOT / "ios/EarthGuardians/UI/Root/OnboardingScreen.swift"
ANDROID_IMAGES = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/common/OfficialSiteImages.kt"
IOS_IMAGES = ROOT / "ios/EarthGuardians/UI/Common/OfficialSiteImages.swift"
REQUIRED_SCREEN_NAMES = [
    "01-home",
    "02-events",
    "03-stories",
    "04-maps",
    "05-more",
    "06-section-donate",
]
APPROVED_IMAGE_HOSTS = {"images.squarespace-cdn.com"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read_text(path: Path) -> str:
    require(path.is_file(), f"required implementation file is missing: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def screenshot_entries(path: Path) -> list[str]:
    text = read_text(path)
    match = re.search(r"declare -a SCREENS=\(\s*(.*?)\s*\)", text, re.DOTALL)
    require(match is not None, f"{path.relative_to(ROOT)} has no SCREENS array")
    return re.findall(r'^\s*"([^\"]+)"\s*$', match.group(1), re.MULTILINE)


def image_catalog(path: Path, pattern: str) -> dict[str, str]:
    text = read_text(path)
    return dict(re.findall(pattern, text))


def check_onboarding_imagery(matrix: dict, android_script: str, ios_script: str) -> None:
    contract = matrix["onboarding"]
    capture = contract["capture"]
    minimum_images = int(contract["minimum_distinct_approved_images"])
    max_alpha = float(contract["maximum_overlay_alpha"])
    android_text = contract["android_expected_text"]

    require(f'shot "{capture}"' in android_script, "Android CI flow must capture onboarding before skipping it")
    require(f'shot "{capture}"' in ios_script, "iOS CI flow must capture onboarding before route launches")
    require(
        f'wait_visible "{android_text}"' in android_script,
        "Android onboarding capture must assert its first-slide feature text is visible",
    )

    android = read_text(ANDROID_ONBOARDING)
    ios = read_text(IOS_ONBOARDING)
    android_refs = re.findall(r"imageUrl\s*=\s*OfficialSiteImages\.([A-Z_]+)", android)
    ios_refs = re.findall(r"imageURL\s*:\s*OfficialSiteImages\.([A-Za-z][A-Za-z0-9_]*)", ios)
    require(len(android_refs) >= minimum_images, "Android onboarding must use approved imagery across multiple slides")
    require(len(ios_refs) >= minimum_images, "iOS onboarding must use approved imagery across multiple slides")

    android_catalog = image_catalog(
        ANDROID_IMAGES,
        r'const val ([A-Z_]+)\s*=\s*"(https://[^\"]+)"',
    )
    ios_catalog = image_catalog(
        IOS_IMAGES,
        r'static let ([A-Za-z][A-Za-z0-9_]*)\s*=\s*"(https://[^\"]+)"',
    )
    require(set(android_refs) <= android_catalog.keys(), "Android onboarding references an image missing from its approved catalog")
    require(set(ios_refs) <= ios_catalog.keys(), "iOS onboarding references an image missing from its approved catalog")

    android_urls = {android_catalog[key] for key in android_refs}
    ios_urls = {ios_catalog[key] for key in ios_refs}
    require(len(android_urls) >= minimum_images, "Android onboarding needs multiple distinct approved image URLs")
    require(len(ios_urls) >= minimum_images, "iOS onboarding needs multiple distinct approved image URLs")
    require(android_urls == ios_urls, "Android/iOS onboarding must share the same approved image set")
    for url in android_urls | ios_urls:
        require(urlparse(url).hostname in APPROVED_IMAGE_HOSTS, f"unapproved onboarding image host: {url}")

    require("AsyncImage(" in android and "ContentScale.Crop" in android, "Android onboarding imagery must be rendered as a cropped image layer")
    require("AsyncImage(url: URL(string: slide.imageURL))" in ios and ".scaledToFill()" in ios, "iOS onboarding imagery must be rendered as a cropped image layer")
    require("contentDescription = slide.imageDescription" in android, "Android onboarding images need accessible descriptions")
    require(".accessibilityLabel(slide.imageDescription)" in ios, "iOS onboarding images need accessible descriptions")

    android_overlay = android.split("AsyncImage(", 1)[1].split("Row(", 1)[0]
    android_alpha = [float(value) for value in re.findall(r"Color\.Black\.copy\(alpha\s*=\s*([0-9.]+)f?\)", android_overlay)]
    ios_overlay = ios.split("AsyncImage(url: URL(string: slide.imageURL))", 1)[1].split("VStack(alignment: .leading)", 1)[0]
    ios_alpha = [float(value) for value in re.findall(r"\.black\.opacity\(([0-9.]+)\)", ios_overlay)]
    require(android_alpha, "Android onboarding image must have a readability overlay")
    require(ios_alpha, "iOS onboarding image must have a readability overlay")
    require(max(android_alpha) <= max_alpha, f"Android image overlay exceeds opacity ceiling {max_alpha}")
    require(max(ios_alpha) <= max_alpha, f"iOS image overlay exceeds opacity ceiling {max_alpha}")
    require(min(android_alpha) <= 0.20 and min(ios_alpha) <= 0.20, "onboarding imagery must retain a visibly low-opacity overlay treatment")


def main() -> int:
    matrix = json.loads(MATRIX_PATH.read_text(encoding="utf-8"))
    screens = matrix.get("screens", [])
    names = [screen["name"] for screen in screens]
    require(names == REQUIRED_SCREEN_NAMES, f"unexpected screen matrix: {names}")
    require(len(names) == len(set(names)), "screen names must be unique")

    android_script = read_text(ANDROID_PATH)
    ios_script = read_text(IOS_PATH)
    android_rows = screenshot_entries(ANDROID_PATH)
    ios_rows = screenshot_entries(IOS_PATH)
    expected_android = [
        "|".join((s["name"], s["android_route"], s["android_expected_text"]))
        for s in screens
    ]
    expected_ios = ["|".join((s["name"], s["ios_route"])) for s in screens]
    require(android_rows == expected_android, "Android screenshot routes/text drifted from the shared matrix")
    require(ios_rows == expected_ios, "iOS screenshot routes drifted from the shared matrix")

    for screen in screens:
        android_route = screen["android_route"]
        require(android_route.startswith("earthguardians://"), f"invalid Android route: {android_route}")
        require(
            android_route.removeprefix("earthguardians://") == screen["ios_route"],
            f"platform route mismatch for {screen['name']}",
        )
        require(bool(screen["android_expected_text"].strip()), f"missing Android UI assertion for {screen['name']}")

    check_onboarding_imagery(matrix, android_script, ios_script)
    print(f"UI parity contract OK: {len(screens)} shared routes plus image-led onboarding")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, KeyError, OSError, json.JSONDecodeError) as error:
        print(f"UI parity contract FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
