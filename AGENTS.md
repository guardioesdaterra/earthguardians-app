# AGENTS.md — Working Agreement for the EarthGuardians app

> **Read this first. Special build rule below. It is not negotiable.**

This repository contains the official Android application for
[EarthGuardians.org](https://www.earthguardians.org) — a youth-led, BIPOC-centered
climate justice nonprofit. We apply the same standard to the app that the
organization applies to its mission: no half-finished work shipped, and
verification always in a reproducible environment.

## Golden rule: CI-only builds

**Never build, test, or lint this project on a local machine. All builds and all
test execution happen on GitHub Actions, and nowhere else.**

Why (it is a deliberate constraint, not a workaround):

- There is no Android SDK, JDK or Gradle distribution on the machines this repo is
  edited on — and none is installed on purpose.
- CI is the only environment that reproduces the exact toolchain
  (JDK 17 + Gradle 8.11.1 + AGP 8.7.3 + Android SDK 35) for every contributor.
- Artifacts produced on CI are the ones that are signed, distributed and released.

### What that means in practice

| Task | Do this |
|---|---|
| Compile the app | Push to a branch / open a PR, then watch the **ci.yml** run |
| Run unit tests | Same — `testDebugUnitTest` is part of the CI workflow |
| Get an APK | `gh run download <run-id>` (debug builds upload an APK artifact) |
| Fix a compile error | Edit code, push again — never run `./gradlew` locally |
| Investigate red CI | Use `gh run view <run-id> --log-failed` (or read the run page on github.com) |
| Cut a release | Push tag `v*` — the release workflow builds from the tag |

### Tooling notes

- This repository intentionally has **no committed Gradle wrapper JAR**;
  `gradle/actions/setup-gradle@v4` handles Gradle 8.11.1 on CI. Developers don't
  need a local Gradle either.
- There is no Android emulator on CI. Unit tests are pure JVM (JUnit 4 +
  `kxml2`/`xmlpull` for the XML parsers) and run fast.
- The iOS project is **never committed as `.xcodeproj`** — XcodeGen generates it
  on CI from `ios/project.yml`. EGCore (`ios/Packages/EGCore`) is pure Foundation
  and is unit-tested with `swift test` on the macOS runners.
- git identity should be configured before the first commit
  (`git config user.name` / `user.email`).

### If you are an AI agent working in this repo

1. You may freely create/edit every Kotlin, XML, HTML, Swift, YAML and Gradle file.
2. You must not attempt `./gradlew`, `kotlinc`, `xcodebuild`, `xcrun`, or any
   local Android/Xcode toolchain — including `swift build`/`swift test` locally.
3. Correctness is the CI run's job to confirm — after pushing meaningful
   changes, wait for the run and iterate on failures until green.
4. If CI is not reachable (no `gh`, no network), document the untested state in
   the commit/PR description — say which files changed and what defect class a
   CI run must confirm.

## Project conventions

- **Package root:** `org.earthguardians.app` / iOS bundle `org.earthguardians.app` (+ `.widget`)
- **Android UI:** Jetpack Compose, single-activity, min SDK 26, target/compile SDK 35.
- **iOS UI:** SwiftUI, iOS deployment per tier (below), XcodeGen project, app +
  WidgetKit extension, AppIntents + CoreSpotlight + BGTaskScheduler.
- **Architecture:** no DI framework — `graph/AppGraph.kt` (Android) and
  `AppGraph.shared` (iOS) are the composition roots.
- **Design language:** iOS-Apple style — large titles, glassmorphism cards,
  bento grids. Palette is strictly black/white + iOS red `#FF3B30` and green
  `#34C759`. Never introduce other hues without updating `ui/theme/Theme.kt`
  (Android) or `ios/EarthGuardians/UI/Theme/EGTheme.swift` (iOS) — the single
  palette sources; both support System/Light/Dark.
- **Site integration:** native screens where content is structured (feeds,
  catalog); themed WebView (`ui/web/WebViewScreen.kt` /
  `ios/EarthGuardians/UI/Web/SiteWebView.swift`) for free-form pages;
  the story reader is a bundled Vue 3 + VueUse shell (`app/src/main/assets/eg_story/index.html`,
  copied into the iOS bundle as `ios/EarthGuardians/Resources/index.html`).
- **Parsers and routing are pure Kotlin / pure Swift** (`data/parse/*`,
  `graph/Deeplinks` in `graph/AppGraph.kt`, `ios/Packages/EGCore`) so they are
  unit-testable on JVM / macOS — keep them free of Android/UIKit imports.
- **Data:** Squarespace RSS (`?format=RSS` per collection) + `/sitemap.xml`,
  OkHttp (Android) / async URLSession (iOS), JSON disk cache, offline-first.

### iOS support tiers (three IPAs per CI run)

| Tier | IPA | Deployment target | Runner / SDK | Kit |
|---|---|---|---|---|
| Compat floor | `EG-iOS16.ipa` | iOS 16.0 | `macos-15` (Xcode 16) | hand-painted glass, `#if`-free iOS 16 APIs |
| Modern | `EG-iOS18.ipa` | iOS 18.0 | `macos-15` (Xcode 16) | + `EG_MODERN`: `ContentUnavailableView`, symbol effects, aligned scroll targets, 2-param `onChange` |
| Latest | `EG-iOS26.ipa` | iOS 26.0 | `macos-26` (Xcode 26) | + `EG_LATEST`: Liquid Glass (`glassEffect`, `.buttonStyle(.glass)`) |

Tier plumbing lives in `ios/EarthGuardians/UI/Design/KitTiers.swift`; the flags
are wired per build configuration in `ios/project.yml`. All other app code
stays on the iOS 16 floor so every tier compiles.

## Testing rules

- Every pure-Kotlin class gets a JUnit test under `app/src/test/kotlin/…`.
  Existing suites: `RssParserTest`, `SitemapParserTest`, `EgTextTest`,
  `DeeplinksTest`, `SiteCatalogTest`.
- The iOS twin lives in the SwiftPM package `ios/Packages/EGCore` with XCTest
  suites under `ios/Packages/EGCore/Tests` (`ParserTests`, `DeeplinkSitemapTests`,
  `CatalogRepoTests`) — run on both `macos-15` and `macos-26` runners.
- Compose/SwiftUI behavior is verified on CI by build success + review; do not
  write Robolectric or instrumentation tests (no emulator/simulator on CI).
- Tests must never hit the network. If you need fixtures, inline them as string
  constants in the test file.

## Builds & releases

- **PR / push → `main`:** `.github/workflows/ci.yml`
  → Android: `assembleDebug`, `assembleRelease`, `testDebugUnitTest`
  → iOS: `swift test` (both runners) + three unsigned IPA tiers
- **Tag `v*`:** `.github/workflows/release.yml`
  → unsigned release APK + AAB and all three iOS IPAs attached to the release
  (signing is provisioned later for Play Store / TestFlight)

Signing is handled outside CI secrets for now; release artifacts are unsigned
builds until keys are provisioned.

## Formatting

- Kotlin guides follow Android's official Kotlin style; `.editorconfig` is present.
- Comments should explain *why*, not *what*.
- No new top-level string colors: use `egPalette` (theme-aware).
