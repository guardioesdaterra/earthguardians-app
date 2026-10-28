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
- git identity should be configured before the first commit
  (`git config user.name` / `user.email`).

### If you are an AI agent working in this repo

1. You may freely create/edit every Kotlin, XML, HTML and Gradle file.
2. You must not attempt `./gradlew`, `kotlinc`, or any local Android toolchain.
3. Correctness is the CI run's job to confirm — after pushing meaningful
   changes, wait for the run and iterate on failures until green.
4. If CI is not reachable (no `gh`, no network), document the untested state in
   the commit/PR description — say which files changed and what defect class a
   CI run must confirm.

## Project conventions

- **Package root:** `org.earthguardians.app`
- **UI:** Jetpack Compose, single-activity, min SDK 26, target/compile SDK 35.
- **Architecture:** no DI framework — `graph/AppGraph.kt` is the composition root,
  provided through `LocalAppGraph` (CompositionLocal).
- **Design language:** iOS-Apple style — large titles, glassmorphism cards,
  bento grids. Palette is strictly black/white + iOS red `#FF3B30` and green
  `#34C759`. Never introduce other hues without updating `ui/theme/Theme.kt`
  (which is the single palette source; supports System/Light/Dark).
- **Site integration:** native screens where content is structured (feeds,
  catalog); themed WebView (`ui/web/WebViewScreen.kt`) for free-form pages;
  the story reader is a bundled Vue 3 + VueUse shell (`assets/eg_story/index.html`).
- **Parsers and routing are pure Kotlin** (`data/parse/*`, `graph/Deeplinks` in
  `graph/AppGraph.kt`) so they are unit-testable on JVM — keep them free of
  Android imports.
- **Data:** Squarespace RSS (`?format=RSS` per collection) + `/sitemap.xml`,
  OkHttp on a background scope, JSON disk cache in `cacheDir` (offline-first).

## Testing rules

- Every pure-Kotlin class gets a JUnit test under `app/src/test/kotlin/…`.
  Existing suites: `RssParserTest`, `SitemapParserTest`, `EgTextTest`,
  `DeeplinksTest`, `SiteCatalogTest`.
- Compose UI behavior is verified on CI by build success + review; do not write
  Robolectric or instrumentation tests (no emulator on CI).
- Tests must never hit the network. If you need fixtures, inline them as string
  constants in the test file.

## Builds & releases

- **PR / push → `main`:** `.github/workflows/ci.yml`
  → `assembleDebug`, `assembleRelease`, `testDebugUnitTest`
- **Tag `v*`:** `.github/workflows/release.yml`
  → builds unsigned release APK (signing is provisioned later for Play Store)

Signing for Play Store distribution is handled outside CI secrets for now;
release artifacts are unsigned AIA-style builds until keys are provisioned.

## Formatting

- Kotlin guides follow Android's official Kotlin style; `.editorconfig` is present.
- Comments should explain *why*, not *what*.
- No new top-level string colors: use `egPalette` (theme-aware).
