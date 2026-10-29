#!/usr/bin/env python3
"""Check shared Android/iOS UI, route, imagery, and native map contracts."""
from __future__ import annotations

import json
import hashlib
import re
import sys
from pathlib import Path
from urllib.parse import quote, unquote, urlparse

ROOT = Path(__file__).resolve().parents[2]
MATRIX_PATH = ROOT / "tests/ui-parity/screen-matrix.json"
ANDROID_PATH = ROOT / ".github/scripts/android-screenshots.sh"
IOS_PATH = ROOT / ".github/scripts/ios-screenshots.sh"
DOC_PATH = ROOT / "docs/official-home-image-sources.md"
ANDROID_ONBOARDING = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/root/OnboardingScreen.kt"
IOS_ONBOARDING = ROOT / "ios/EarthGuardians/UI/Root/OnboardingScreen.swift"
ANDROID_IMAGES = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/common/OfficialSiteImages.kt"
IOS_IMAGES = ROOT / "ios/EarthGuardians/UI/Common/OfficialSiteImages.swift"
ANDROID_COLLAGE = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/common/OfficialSitePhotoCollage.kt"
IOS_COLLAGE = ROOT / "ios/EarthGuardians/UI/Common/OfficialSitePhotoCollage.swift"
ANDROID_HOME = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/home/HomeScreen.kt"
IOS_HOME = ROOT / "ios/EarthGuardians/UI/Home/HomeScreen.swift"
ANDROID_ACTIVITY = ROOT / "app/src/main/kotlin/org/earthguardians/app/MainActivity.kt"
ANDROID_APP_ROOT = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/root/EgAppRoot.kt"
IOS_APP = ROOT / "ios/EarthGuardians/App/EarthGuardiansApp.swift"
IOS_ROOT = ROOT / "ios/EarthGuardians/UI/Root/RootView.swift"
ANDROID_MAPS = ROOT / "app/src/main/kotlin/org/earthguardians/app/ui/maps/MapsScreen.kt"
IOS_MAPS = ROOT / "ios/EarthGuardians/UI/Maps/MapsScreen.swift"
ANDROID_MAP_REPOSITORY = ROOT / "app/src/main/kotlin/org/earthguardians/app/data/repo/MapAssetRepository.kt"
ANDROID_RESOURCE_LIBRARY = ROOT / "app/src/main/kotlin/org/earthguardians/app/data/repo/ResourceLibrary.kt"
IOS_RESOURCE_LIBRARY = ROOT / "ios/Packages/EGCore/Sources/EGCore/ResourceLibrary.swift"
ANDROID_MAP_ASSETS = ROOT / "app/src/main/assets/maps"
IOS_MAP_ASSETS = ROOT / "ios/EarthGuardians/Resources/Maps"
REQUIRED_SCREEN_NAMES = [
    "01-home",
    "02-events",
    "03-stories",
    "04-maps",
    "05-more",
    "06-section-nested-query",
    "07-bare-root-host",
]
BARE_ROOT_SCREEN_NAME = "07-bare-root-host"


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


def canonical_route(route: str) -> str:
    section_prefix = "section?u="
    if not route.startswith(section_prefix):
        return route

    destination = route.removeprefix(section_prefix)
    if re.match(r"^https?%3A%2F%2F", destination, re.IGNORECASE):
        destination = unquote(destination)
    return section_prefix + quote(destination, safe="-._~")


def check_onboarding_capture(matrix: dict, android_script: str, ios_script: str) -> None:
    contract = matrix["onboarding"]
    capture = contract["capture"]
    require(f'shot "{capture}"' in android_script, "Android CI flow must capture onboarding before continuing")
    require(f'shot "{capture}"' in ios_script, "iOS CI flow must capture onboarding before route launches")
    require(f'wait_visible "{contract["android_expected_text"]}"' in android_script,
            "Android onboarding capture must assert its first-slide feature text")


def check_image_catalog(matrix: dict) -> tuple[dict[str, str], dict[str, str]]:
    images = matrix["official_images"]
    require(len(images) == 6, "the six official homepage photos must be bundled as local assets for both platforms")
    keys = [image["key"] for image in images]
    require(len(keys) == len(set(keys)), "official image keys must be unique")

    android_text = read_text(ANDROID_IMAGES)
    ios_text = read_text(IOS_IMAGES)
    android_catalog = dict(re.findall(r"\bval\s+([A-Z_]+)\s*=\s*R\.drawable\.(eg_home_[a-z_]+)", android_text))
    ios_catalog = dict(re.findall(r'\bstatic let\s+([A-Za-z][A-Za-z0-9]*)\s*=\s*"(EGHome[A-Za-z]+)"', ios_text))
    android_key_by_symbol: dict[str, str] = {}
    ios_key_by_symbol: dict[str, str] = {}
    docs = read_text(DOC_PATH)

    for image in images:
        key = image["key"]
        android_symbol = image["android_symbol"]
        ios_symbol = image["ios_symbol"]
        require(android_catalog.get(android_symbol) == image["android_resource"],
                f"Android image catalog drifted for {key}")
        require(ios_catalog.get(ios_symbol) == image["ios_asset_name"],
                f"iOS image catalog drifted for {key}")
        android_key_by_symbol[android_symbol] = key
        ios_key_by_symbol[ios_symbol] = key

        android_asset = ROOT / image["android_path"]
        ios_asset = ROOT / image["ios_path"]
        require(android_asset.is_file(), f"missing Android image asset: {image['android_path']}")
        require(ios_asset.is_file(), f"missing iOS image asset: {image['ios_path']}")
        contents = json.loads(read_text(ios_asset.parent / "Contents.json"))
        filenames = [entry.get("filename") for entry in contents.get("images", [])]
        require(ios_asset.name in filenames, f"iOS asset catalog does not register {ios_asset.name}")
        require(hashlib.sha256(android_asset.read_bytes()).digest() == hashlib.sha256(ios_asset.read_bytes()).digest(),
                f"Android and iOS image bytes differ for {key}")

        doc_rows = [line for line in docs.splitlines()
                    if f"`{image['android_path']}`" in line and f"`{image['ios_path']}`" in line]
        require(len(doc_rows) == 1, f"image source document must map both platform assets for {key}")
        source_urls = re.findall(r"https://[^|\s`]+", doc_rows[0])
        require(len(source_urls) == 1, f"image source document must give one source URL for {key}")
        require(urlparse(source_urls[0]).hostname == matrix["approved_image_host"],
                f"unapproved official image host for {key}: {source_urls[0]}")

    android_list = re.search(r"\bCOLLAGE\s*=\s*listOf\((.*?)\)", android_text, re.DOTALL)
    ios_list = re.search(r"\bstatic let collage\s*=\s*\[(.*?)\]", ios_text, re.DOTALL)
    require(android_list is not None, "Android official image catalog must expose COLLAGE")
    require(ios_list is not None, "iOS official image catalog must expose collage")
    expected_android = [image["android_symbol"] for image in images]
    expected_ios = [image["ios_symbol"] for image in images]
    require(re.findall(r"\b[A-Z][A-Z_]+\b", android_list.group(1)) == expected_android,
            "Android collage image order differs from the documented set")
    require(re.findall(r"\b[a-z][A-Za-z0-9]*\b", ios_list.group(1)) == expected_ios,
            "iOS collage image order differs from the documented set")

    android_collage = read_text(ANDROID_COLLAGE)
    ios_collage = read_text(IOS_COLLAGE)
    require("OfficialSiteImages.COLLAGE[" in android_collage and "alpha = opacity" in android_collage,
            "Android photo collage must render from the shared catalog with caller-controlled opacity")
    require("OfficialSiteImages.collage[" in ios_collage and ".opacity(opacity)" in ios_collage,
            "iOS photo collage must render from the shared catalog with caller-controlled opacity")
    default_opacity = matrix["onboarding"]["collage_opacity"]["default_background"]
    require(f"opacity: Float = {default_opacity}f" in android_collage,
            "Android photo collage background opacity must default to 7.5%")
    require(f"var opacity: Double = {default_opacity}" in ios_collage,
            "iOS photo collage background opacity must default to 7.5%")
    require("contentDescription = null" in android_collage,
            "Android collage photos must remain decorative to accessibility")
    require(".accessibilityHidden(true)" in ios_collage,
            "iOS collage photos must remain decorative to accessibility")
    return android_key_by_symbol, ios_key_by_symbol


def check_image_views(matrix: dict, android_key_by_symbol: dict[str, str], ios_key_by_symbol: dict[str, str]) -> None:
    onboarding = matrix["onboarding"]
    android_home = read_text(ANDROID_HOME)
    ios_home = read_text(IOS_HOME)
    android = read_text(ANDROID_ONBOARDING)
    ios = read_text(IOS_ONBOARDING)

    hero = next(image for image in matrix["official_images"] if image["key"] == onboarding["home_hero_image"])
    require(f"OfficialSiteImages.{hero['android_symbol']}" in android_home,
            "Android home hero must use the documented official image")
    require(f"Image(OfficialSiteImages.{hero['ios_symbol']})" in ios_home,
            "iOS home hero must use the documented official image")
    require("OfficialSitePhotoCollage(" in android_home and "OfficialSitePhotoCollage(" in ios_home,
            "both home screens must include the official photo collage")

    android_refs = re.findall(r"imageRes\s*=\s*OfficialSiteImages\.([A-Z_]+)", android)
    ios_refs = re.findall(r"imageName\s*=\s*OfficialSiteImages\.([A-Za-z][A-Za-z0-9]*)", ios)
    expected = onboarding["slide_images"]
    require([android_key_by_symbol.get(ref) for ref in android_refs] == expected,
            "Android onboarding image sequence differs from the documented cross-platform sequence")
    require([ios_key_by_symbol.get(ref) for ref in ios_refs] == expected,
            "iOS onboarding image sequence differs from the documented cross-platform sequence")
    require("painterResource(slide.imageRes)" in android and "ContentScale.Crop" in android,
            "Android onboarding must crop its locally bundled slide images")
    require("contentDescription = slide.imageDescription" in android,
            "Android onboarding slide images need accessible descriptions")
    require("Image(slide.imageName)" in ios and ".scaledToFill()" in ios,
            "iOS onboarding must crop its locally bundled slide images")
    require(".accessibilityLabel(slide.imageDescription)" in ios,
            "iOS onboarding slide images need accessible descriptions")
    require("Brush.verticalGradient" in android and "LinearGradient(" in ios,
            "onboarding photo panels need text-legibility gradients on both platforms")

    opacity = onboarding["collage_opacity"]
    require(f"opacity = {opacity['home_hero']:.2f}f" in android_home and f"opacity: {opacity['home_hero']:.2f}" in ios_home,
            "home hero collage opacity must remain 15% on both platforms")
    require(f"alpha = {onboarding['home_hero_photo_alpha']:.2f}f" in android_home and
            f".opacity({onboarding['home_hero_photo_alpha']:.2f})" in ios_home,
            "home hero photo alpha must remain aligned across platforms")

    check_onboarding_photo_sources(android, ios)
    check_onboarding_photo_layouts(matrix, android_key_by_symbol, ios_key_by_symbol, android, ios)


def check_onboarding_photo_sources(android: str, ios: str) -> None:
    remote_image_patterns = (
        r"\bAsyncImage\s*\(",
        r"\brememberAsyncImagePainter\s*\(",
        r"\brememberImagePainter\s*\(",
        r"\bSubcomposeAsyncImage\s*\(",
        r"\bGlideImage\s*\(",
        r"\bURLSession\b",
        r"\bURL\s*\(\s*string\s*:",
        r"\bimage(?:Url|URL)\b",
    )
    # This restriction applies only to the fixed onboarding photo views. Dynamic
    # feed thumbnails and images inside native WebViews remain remote by design.
    sources = (("Android onboarding photo renderer", android),
               ("iOS onboarding photo renderer", ios))
    for label, source in sources:
        require(re.search(r"https?://", source, re.IGNORECASE) is None,
                f"{label} must not embed external photo URLs")
        require(not any(re.search(pattern, source) for pattern in remote_image_patterns),
                f"{label} must render bundled local assets, not a network image source")


def source_section(source: str, start_marker: str, end_marker: str, label: str) -> str:
    start = source.find(start_marker)
    require(start >= 0, f"{label} is missing its structural start marker")
    end = source.find(end_marker, start + len(start_marker))
    require(end > start, f"{label} is missing its structural end marker")
    return source[start:end]


def check_home_logo(matrix: dict) -> None:
    contract = matrix["home_logo"]
    android_spec = contract["android"]
    ios_spec = contract["ios"]
    android_home = read_text(ANDROID_HOME)
    ios_home = read_text(IOS_HOME)
    android_header = source_section(
        android_home,
        "        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .statusBarsPadding()",
        "        LazyColumn(",
        "Android Home header",
    )
    ios_header = source_section(
        ios_home, "private struct HomeChrome: ViewModifier", "private extension View", "iOS Home header")

    android_dark = f"R.drawable.{android_spec['dark_resource']}"
    android_light = f"R.drawable.{android_spec['light_resource']}"
    require("Image(" in android_header and "painterResource(" in android_header and
            f"if (palette.isDark) {android_dark} else {android_light}" in android_header and
            "contentScale = ContentScale.Fit" in android_header and
            f".size({contract['size_dp']}.dp)" in android_header,
            "Android Home header must render the official logo with a light/dark resource selected by the active theme")
    require(f'Image("{ios_spec["asset_name"]}")' in ios_header and
            ".resizable()" in ios_header and ".scaledToFit()" in ios_header and
            f".frame(width: {contract['size_dp']}, height: {contract['size_dp']})" in ios_header and
            ".accessibilityHidden(true)" in ios_header,
            "iOS Home header must render the official theme-aware logo asset as a fitted decorative mark")

    android_dark_path = ROOT / android_spec["dark_path"]
    android_light_path = ROOT / android_spec["light_path"]
    ios_asset_dir = ROOT / ios_spec["asset_directory"]
    ios_dark_path = ios_asset_dir / ios_spec["dark_filename"]
    ios_light_path = ios_asset_dir / ios_spec["light_filename"]
    for path in (android_dark_path, android_light_path, ios_dark_path, ios_light_path):
        require(path.is_file(), f"missing bundled Home logo variant: {path.relative_to(ROOT)}")
    contents = json.loads(read_text(ios_asset_dir / "Contents.json"))
    entries = {entry.get("filename"): entry for entry in contents.get("images", [])}
    light_entry = entries.get(ios_spec["light_filename"], {})
    dark_entry = entries.get(ios_spec["dark_filename"], {})
    require(light_entry.get("idiom") == "universal" and "appearances" not in light_entry,
            "iOS Home logo light/default variant must be registered as the universal asset")
    require(dark_entry.get("idiom") == "universal" and
            {appearance.get("appearance"): appearance.get("value")
             for appearance in dark_entry.get("appearances", [])} == {"luminosity": "dark"},
            "iOS Home logo dark variant must be registered for dark luminosity")
    require(hashlib.sha256(android_light_path.read_bytes()).digest() == hashlib.sha256(ios_light_path.read_bytes()).digest() and
            hashlib.sha256(android_dark_path.read_bytes()).digest() == hashlib.sha256(ios_dark_path.read_bytes()).digest(),
            "Android and iOS Home logo variants must use matching local image bytes")


def check_onboarding_photo_layouts(
    matrix: dict,
    android_key_by_symbol: dict[str, str],
    ios_key_by_symbol: dict[str, str],
    android: str,
    ios: str,
) -> None:
    onboarding = matrix["onboarding"]
    layout = onboarding["photo_layouts"]
    images_by_key = {image["key"]: image for image in matrix["official_images"]}
    grid_keys = onboarding["photo_grid_images"]
    expected_catalog_order = [image["key"] for image in matrix["official_images"]]
    require(grid_keys == expected_catalog_order,
            "onboarding photo grid must use the complete six-photo local catalog in order")
    expected_android_grid = [images_by_key[key]["android_symbol"] for key in grid_keys]
    expected_ios_grid = [images_by_key[key]["ios_symbol"] for key in grid_keys]

    android_grid_list = re.search(r"private val onboardingGridImages\s*=\s*listOf\((.*?)\)", android, re.DOTALL)
    require(android_grid_list is not None, "Android onboarding must define its local photo-grid image list")
    android_grid_symbols = re.findall(r"OfficialSiteImages\.([A-Z_]+)", android_grid_list.group(1))
    require(android_grid_symbols == expected_android_grid and
            [android_key_by_symbol.get(symbol) for symbol in android_grid_symbols] == grid_keys,
            "Android onboarding grid must reference the six bundled catalog photos in matrix order")
    require([ios_key_by_symbol.get(image["ios_symbol"]) for image in matrix["official_images"]] == expected_ios_grid,
            "iOS onboarding grid must use the six bundled catalog photos in matrix order")

    android_backdrop = source_section(
        android, "private fun OnboardingBackdrop(", "private fun OnboardingPhotoGrid(", "Android onboarding backdrop")
    android_wallpaper_grid = source_section(
        android, "private fun OnboardingPhotoGrid(", "private fun OnboardingArtwork(", "Android wallpaper photo grid")
    android_artwork = source_section(
        android, "private fun OnboardingArtwork(", "private fun BoxScope.OnboardingGridArtwork(", "Android slide artwork selector")
    android_slide_grid = source_section(
        android, "private fun BoxScope.OnboardingGridArtwork(", "private fun BoxScope.OnboardingOffsetArtwork(", "Android 2x2 slide photo grid")
    android_offset = source_section(
        android, "private fun BoxScope.OnboardingOffsetArtwork(", "/** Exposed for tests", "Android asymmetric photo band")

    ios_backdrop = source_section(
        ios, "private var onboardingBackdrop: some View", "private var canopyGlow", "iOS onboarding backdrop")
    ios_slide_view = source_section(
        ios, "private func slideView(_ slide:", "private func slideArtwork(_ slide:", "iOS onboarding slide view")
    ios_artwork = source_section(
        ios, "private func slideArtwork(_ slide:", "private func gridArtwork(_ slide:", "iOS slide artwork selector")
    ios_slide_grid = source_section(
        ios, "private func gridArtwork(_ slide:", "private func standardArtwork(_ slide:", "iOS 2x2 slide photo grid")
    ios_offset = source_section(
        ios, "private func offsetArtwork(_ slide:", "private var photoCredit", "iOS asymmetric photo band")
    ios_grid_source = read_text(IOS_COLLAGE)
    ios_wallpaper_grid = source_section(
        ios_grid_source, "struct OfficialSitePhotoGrid: View", "struct OfficialSitePhotoCollage: View", "iOS wallpaper photo grid")

    grid_slide = layout["grid_slide"]
    full_bleed_slide = layout["full_bleed_slide"]
    asymmetric_slide = layout["asymmetric_band_slide"]
    require([grid_slide, full_bleed_slide, asymmetric_slide] == [0, 1, 2],
            "photo treatments must remain mapped to the first three onboarding slides")

    backdrop_grid = layout["backdrop_grid"]
    rows, columns = backdrop_grid["rows"], backdrop_grid["columns"]
    require(re.search(r"repeat\s*\(\s*" + str(rows) + r"\s*\)\s*\{\s*row\s*->", android_wallpaper_grid) is not None and
            re.search(r"repeat\s*\(\s*" + str(columns) + r"\s*\)\s*\{\s*column\s*->", android_wallpaper_grid) is not None and
            "painterResource(onboardingGridImages[imageIndex])" in android_wallpaper_grid and
            "ContentScale.Crop" in android_wallpaper_grid and ".fillMaxHeight()" in android_wallpaper_grid,
            "Android 5x3 wallpaper grid must crop tiles from bundled onboarding photo assets")
    require(f"private let rows = {rows}" in ios_wallpaper_grid and
            f"private let columns = {columns}" in ios_wallpaper_grid and
            "ForEach(0..<rows" in ios_wallpaper_grid and "ForEach(0..<columns" in ios_wallpaper_grid and
            "Image(OfficialSiteImages.collage[index])" in ios_wallpaper_grid and
            ".scaledToFill()" in ios_wallpaper_grid and ".clipped()" in ios_wallpaper_grid,
            "iOS 5x3 wallpaper grid must crop tiles from bundled catalog photos")
    require(f"OfficialSitePhotoGrid(opacity: {layout['wallpaper_grid_opacity']:.2f}" in ios_backdrop,
            "iOS onboarding wallpaper must render the local photo grid")
    require("OnboardingPhotoGrid(" in android_backdrop and
            f"opacity = {layout['wallpaper_grid_opacity']:.2f}f" in android_backdrop,
            "Android onboarding wallpaper must render the local photo grid")
    require(f"opacity = {layout['backdrop_collage_opacity']:.2f}f" in android_backdrop and
            f"opacity: {layout['backdrop_collage_opacity']:.2f}" in ios_backdrop,
            "onboarding wallpaper collage must keep its subdued photo opacity on both platforms")

    artwork_rows, artwork_columns = layout["slide_grid"]["rows"], layout["slide_grid"]["columns"]
    require(f"{grid_slide} -> OnboardingGridArtwork()" in android_artwork and
            re.search(r"repeat\s*\(\s*" + str(artwork_rows) + r"\s*\)\s*\{\s*row\s*->", android_slide_grid) is not None and
            re.search(r"repeat\s*\(\s*" + str(artwork_columns) + r"\s*\)\s*\{\s*column\s*->", android_slide_grid) is not None and
            "painterResource(onboardingGridImages[imageIndex])" in android_slide_grid and
            "ContentScale.Crop" in android_slide_grid and ".clip(RoundedCornerShape" in android_slide_grid,
            "Android first-slide artwork must be a cropped local 2x2 photo grid")
    require(f"case {grid_slide}:" in ios_artwork and "gridArtwork(slide)" in ios_artwork and
            ios_slide_grid.count("ForEach(0..<2, id: \\.self)") == 2 and
            "Image(OfficialSiteImages.collage[index])" in ios_slide_grid and
            ".scaledToFill()" in ios_slide_grid and ".clipped()" in ios_slide_grid,
            "iOS first-slide artwork must be a cropped local 2x2 photo grid")

    bleed_key = onboarding["slide_images"][full_bleed_slide]
    bleed_image = images_by_key[bleed_key]
    android_full_bleed = source_section(
        android_backdrop, f"{full_bleed_slide} -> Image(", "else -> OfficialSitePhotoCollage", "Android full-bleed photo frame")
    ios_full_bleed = source_section(ios_backdrop, f"case {full_bleed_slide}:", "default:", "iOS full-bleed photo frame")
    require(f"painterResource(OfficialSiteImages.{bleed_image['android_symbol']})" in android_full_bleed and
            "ContentScale.Crop" in android_full_bleed and ".fillMaxSize()" in android_full_bleed and
            f"alpha = {layout['full_bleed_background_alpha']:.2f}f" in android_full_bleed and
            re.search(rf"\bif\s*\(page\s*==\s*{full_bleed_slide}\)", android_artwork) is not None and
            "FULL-BLEED PHOTO" in android_artwork,
            "Android full-bleed slide must fill its frame with the slide's bundled local photo")
    require(f"Image(slides[{full_bleed_slide}].imageName)" in ios_full_bleed and
            ".resizable()" in ios_full_bleed and ".scaledToFill()" in ios_full_bleed and
            ".frame(width: geometry.size.width, height: geometry.size.height)" in ios_full_bleed and
            ".clipped()" in ios_full_bleed and
            f".opacity({layout['full_bleed_background_alpha']:.2f})" in ios_full_bleed and
            f"slide.id == {full_bleed_slide}" in ios_slide_view and "FULL-BLEED PHOTO" in ios_slide_view,
            "iOS full-bleed slide must fill its frame with the slide's bundled local photo")

    require(f"{asymmetric_slide} -> OnboardingOffsetArtwork(slide = slide)" in android_artwork and
            len(re.findall(r"\bImage\s*\(", android_offset)) == layout["asymmetric_photo_count"] and
            all(f"painterResource(OfficialSiteImages.{symbol})" in android_offset for symbol in (
                images_by_key[onboarding["slide_images"][asymmetric_slide]]["android_symbol"],
                images_by_key["youth_gathering"]["android_symbol"],
                images_by_key["leaves"]["android_symbol"],
            )) and
            all(anchor in android_offset for anchor in ("Alignment.BottomStart", "Alignment.TopEnd", "Alignment.BottomEnd")) and
            ".fillMaxWidth(0.72f)" in android_offset and ".fillMaxWidth(0.48f)" in android_offset and
            ".width(92.dp)" in android_offset,
            "Android third-slide photo band must compose three locally bundled images asymmetrically")
    require(f"case {asymmetric_slide}:" in ios_artwork and "offsetArtwork(slide)" in ios_artwork and
            len(re.findall(r"\bImage\s*\(", ios_offset)) == layout["asymmetric_photo_count"] and
            all(source in ios_offset for source in (
                "Image(slide.imageName)",
                "Image(OfficialSiteImages.youthGathering)",
                "Image(OfficialSiteImages.leaves)",
            )) and
            ios_offset.count(".position(") == layout["asymmetric_photo_count"] and
            "geometry.size.width * 0.72" in ios_offset and "geometry.size.width * 0.44" in ios_offset and
            ".frame(width: 92, height: 68)" in ios_offset and ".scaledToFill()" in ios_offset,
            "iOS third-slide photo band must compose three locally bundled images asymmetrically")

    android_progress = source_section(
        android, "slides.forEachIndexed { index, _ ->", "\n            Row(\n                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)", "Android clickable progress indicators")
    ios_progress = source_section(
        ios, "HStack(spacing: 2) {", "\n            .padding(.vertical, 8)", "iOS clickable progress indicators")
    target_dp = layout["progress_hit_area_dp"]
    require(".clickable {" in android_progress and "pagerState.animateScrollToPage(index)" in android_progress and
            f".width({target_dp}.dp)" in android_progress and f".height({target_dp}.dp)" in android_progress and
            "contentDescription = \"Page ${index + 1} of ${slides.size}\"" in android_progress and
            "selected = active" in android_progress and "role = Role.Tab" in android_progress,
            "Android onboarding progress indicators must be directly clickable, page-selecting, and accessible")
    require("ForEach(slides)" in ios_progress and "Button {" in ios_progress and
            "page = slide.id" in ios_progress and
            f".frame(width: {target_dp}, height: {target_dp})" in ios_progress and
            ".contentShape(Circle())" in ios_progress and
            ".accessibilityLabel(\"Step \\(slide.id + 1) of \\(slides.count)\")" in ios_progress and
            ".accessibilityValue(active ? \"Current\" : \"Not selected\")" in ios_progress,
            "iOS onboarding progress indicators must be directly clickable, page-selecting, and accessible")


def check_startup_handoff(matrix: dict, android_script: str, ios_script: str) -> None:
    contract = matrix["onboarding"]["direct_page_argument"]
    route = contract["route"]
    expected_text = contract["expected_text"]
    custom_url = f"earthguardians://{route}"
    require(custom_url in android_script and 'deeplink "$ONBOARDING_DIRECT_ROUTE"' in android_script,
            "Android CI must cold-launch a direct page argument before onboarding")
    require(f'wait_visible "{expected_text}"' in android_script,
            "Android CI must verify the direct destination after completing onboarding")
    require(route in ios_script and '-onboarding_complete 0 -eg_launch_route "$ONBOARDING_DIRECT_ROUTE"' in ios_script,
            "iOS CI must launch the direct page argument while first-run onboarding is active")

    android_activity = read_text(ANDROID_ACTIVITY)
    android_root = read_text(ANDROID_APP_ROOT)
    android_main_tabs = android_root.split("fun MainTabs", 1)[-1]
    require("routeBus.postRoute(Deeplinks.parseRoute(intent?.dataString" in android_activity,
            "Android activity must enqueue the incoming direct page argument")
    require("false -> OnboardingScreen(" in android_root and "else -> MainTabs(graph)" in android_root,
            "Android route consumer must remain behind the onboarding gate")
    require("graph.routeBus.pending.collectLatest" in android_main_tabs and "navController.navigate(route)" in android_main_tabs,
            "Android must navigate to the queued page argument after onboarding")

    ios_app = read_text(IOS_APP)
    ios_root = read_text(IOS_ROOT)
    require('"eg_launch_route"' in ios_app and "Deeplinks.parseRoute" in ios_app and "graph.routeBus.post" in ios_app,
            "iOS cold launch must parse and enqueue the direct page argument")
    gate = "guard graph.onboarded == true, let raw = graph.routeBus.pending else { return }"
    require('launchArgument(named: "eg_launch_route")' in ios_app,
            "iOS screenshot launch must deliver the argument before onboarding begins")
    require(gate in ios_root, "iOS must retain a cold-start page argument until onboarding is complete")
    require(ios_root.index(gate) < ios_root.index("graph.routeBus.consume()"),
            "iOS must not consume the pending page argument before the onboarding gate")
    require("guard let route = Route.from(raw: raw) else { return }" in ios_root and
            "case .section, .settings, .notifications:" in ios_root,
            "iOS must route the retained page argument to its section destination")


def check_native_maps(matrix: dict) -> None:
    contract = matrix["native_maps"]
    picker_options = contract["picker_options"]
    public_grants_path = contract["public_grants_path"]
    private_manager_path = contract["private_manager_path"]
    datasets = contract["datasets"]

    android_maps = read_text(ANDROID_MAPS)
    ios_maps = read_text(IOS_MAPS)
    android_picker = re.search(r"EgSegmentedControl\(\s*options\s*=\s*listOf\((.*?)\)", android_maps, re.DOTALL)
    ios_picker = re.search(r"EgSegmented\(options:\s*\[(.*?)\]", ios_maps, re.DOTALL)
    require(android_picker is not None, "Android native Maps screen must expose its dataset picker")
    require(ios_picker is not None, "iOS native Maps screen must expose its dataset picker")
    android_options = re.findall(r'"([^"]+)"', android_picker.group(1))
    ios_options = re.findall(r'"([^"]+)"', ios_picker.group(1))
    require(android_options == picker_options,
            "Android public map picker must contain only Crews and Project Grants")
    require(ios_options == picker_options,
            "iOS public map picker must contain only Crews and Project Grants")

    require(private_manager_path not in android_maps.lower() and private_manager_path not in ios_maps.lower(),
            "the manager-only grants route must not appear in either public native map picker")
    android_library = read_text(ANDROID_RESOURCE_LIBRARY)
    ios_library = read_text(IOS_RESOURCE_LIBRARY)
    require(f'id = "project-grants"' in android_library and
            f'canonicalPath = "{public_grants_path}"' in android_library,
            "Android resource metadata must keep Project Grants on the public /project-grants route")
    require(f'id: "project-grants"' in ios_library and
            f'canonicalPath: "{public_grants_path}"' in ios_library,
            "iOS resource metadata must keep Project Grants on the public /project-grants route")

    android_repository = read_text(ANDROID_MAP_REPOSITORY)
    for dataset in datasets:
        require(f'"maps/{dataset}.json"' in android_repository,
                f"Android native maps must load the public {dataset} snapshot")
        require(f'MapsDataLoader.load("{dataset}"' in ios_maps,
                f"iOS native maps must load the public {dataset} snapshot")

        android_data = json.loads(read_text(ANDROID_MAP_ASSETS / f"{dataset}.json"))
        ios_data = json.loads(read_text(IOS_MAP_ASSETS / f"{dataset}.json"))
        required_fields = set(datasets[dataset])
        for platform, records in (("Android", android_data), ("iOS", ios_data)):
            require(isinstance(records, list) and records,
                    f"{platform} {dataset} snapshot must contain public map records")
            require(all(isinstance(record, dict) and required_fields.issubset(record)
                        for record in records),
                    f"{platform} {dataset} records are missing required public fields")
        require(android_data == ios_data,
                f"Android and iOS public {dataset} snapshots must stay in sync")

    require("if (tab == 0) MapMode.Crews else MapMode.Grants" in android_maps and
            'options = listOf("Crews", "Project Grants")' in android_maps,
            "Android picker selections must switch between native crews and grant maps")
    require("if selection == 0" in ios_maps and "CrewsMapScreen()" in ios_maps and
            "ProjectGrantsMapScreen()" in ios_maps,
            "iOS picker selections must open both native public map datasets")
    require("selected = null" in android_maps and "query = \"\"" in android_maps,
            "Android must clear stale marker details and searches when changing map datasets")
    for token in (
        "if (mode == MapMode.Crews)",
        "currentData.regions.filter(::matches)",
        "currentData.crews.forEachIndexed",
        'add(currentMap, "region:${region.id}"',
        'add(currentMap, "crew:$index"',
        "currentData.grants.forEachIndexed",
        'add(currentMap, "grant:$index"',
    ):
        require(token in android_maps, f"Android map mode is missing marker rendering: {token}")

    android_root = read_text(ANDROID_APP_ROOT)
    ios_root = read_text(IOS_ROOT)
    require("composable(Routes.MAPS)" in android_root and "MapsScreen(" in android_root,
            "Android Maps tab must open the native map screen")
    require("MapsScreen {" in ios_root and ".tag(Tab.maps)" in ios_root,
            "iOS Maps tab must open the native map screen")

    for token in (
        "readyMap.setOnMarkerClickListener",
        "selectionCallback?.invoke(selectionFor(marker.title))",
        'key?.startsWith("region:")',
        'key?.startsWith("crew:")',
        'key?.startsWith("grant:")',
        "MapSelection::Region",
        "MapSelection::Crew",
        "MapSelection::Grant",
        "if (selected != null) SelectionCard(selected)",
        "is MapSelection.Region",
        "is MapSelection.Crew",
        "is MapSelection.Grant",
    ):
        require(token in android_maps, f"Android map marker/detail contract is missing: {token}")
    for token in (
        "selection.value.region",
        "selection.value.activeCrews",
        "selection.value.name",
        "selection.value.status",
        "selection.value.project_title",
        "selection.value.country_province",
        "selection.value.direct_beneficiaries",
        "selection.value.indirect_beneficiaries",
    ):
        require(token in android_maps, f"Android map detail panel is missing field: {token}")

    ios_crews = source_section(
        ios_maps, "private struct CrewsMapScreen", "// MARK: - Project grants",
        "iOS crews map")
    for token in (
        "if let selectedLocation",
        "crewLocationCard(selectedLocation)",
        "else if let selectedRegion",
        "crewRegionCard(selectedRegion)",
    ):
        require(token in ios_crews, f"iOS crews map detail selection is missing: {token}")
    ios_locations = source_section(
        ios_maps, "private var crewLocationsMap: some View", "private var crewRegionsMap: some View",
        "iOS individual crews map")
    ios_regions = source_section(
        ios_maps, "private var crewRegionsMap: some View", "private func load()",
        "iOS crew regions map")
    ios_grants = source_section(
        ios_maps, "private struct ProjectGrantsMapScreen", "// MARK: - Shared map UI",
        "iOS project grants map")
    for source, tokens, label in (
        (ios_locations, ("MapAnnotation", "selectedLocation = item", "selectedRegion = nil"), "individual crew marker"),
        (ios_regions, ("MapAnnotation", "selectedRegion = item", "selectedLocation = nil"), "crew region marker"),
        (ios_grants, ("MapAnnotation", "selectedGrant = grant", "if let selectedGrant", "detailCard(title: selectedGrant.project_title"), "project grant marker"),
    ):
        for token in tokens:
            require(token in source, f"iOS {label} selection/detail contract is missing: {token}")
    for token in (
        'detailCard(title: item.region',
        'detailRow("Active crews"',
        'detailCard(title: item.name',
        'detailRow("Status"',
        'detailRow("Region"',
        'detailRow("Direct beneficiaries"',
        'detailRow("Indirect beneficiaries"',
        "selectedGrant.country_province",
    ):
        require(token in ios_maps, f"iOS map detail panel is missing field: {token}")


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
        if screen["name"] == BARE_ROOT_SCREEN_NAME:
            android_url = urlparse(android_route)
            require(
                android_url.scheme == "https" and android_url.netloc == "earthguardians.org" and
                android_url.path == "" and not android_url.params and not android_url.query and
                not android_url.fragment,
                "bare-root Android route must be the raw official root URL",
            )
            ios_route = screen["ios_route"]
            require(
                ios_route.startswith("section?u=") and bool(ios_route.removeprefix("section?u=")),
                "bare-root iOS route must use a non-empty section?u= argument",
            )
            require(
                screen["android_expected_text"] == "",
                "bare-root Android capture must not assert an unverified screen title",
            )
            continue

        require(android_route.startswith("earthguardians://"), f"invalid Android route: {android_route}")
        require(
            canonical_route(android_route.removeprefix("earthguardians://")) == canonical_route(screen["ios_route"]),
            f"platform route mismatch for {screen['name']}",
        )
        require(bool(screen["android_expected_text"].strip()), f"missing Android UI assertion for {screen['name']}")

    check_onboarding_capture(matrix, android_script, ios_script)
    android_key_by_symbol, ios_key_by_symbol = check_image_catalog(matrix)
    check_image_views(matrix, android_key_by_symbol, ios_key_by_symbol)
    check_home_logo(matrix)
    check_startup_handoff(matrix, android_script, ios_script)
    check_native_maps(matrix)
    print(f"UI parity contract OK: {len(screens)} shared routes, six local photos, theme-aware Home logo, cold-start handoff, and native public maps")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, KeyError, OSError, json.JSONDecodeError) as error:
        print(f"UI parity contract FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
