# EarthGuardians — Official App (Android + iOS + Web)

The official app of [EarthGuardians.org](https://www.earthguardians.org)
— a youth-led, BIPOC-centered climate justice organization ("You can be the
single drop of water that creates an enormous ripple effect" — Emmy Scott,
Executive Director).

Two native apps and a static web app in one repository, sharing the Earth
Guardians feeds, resource hub, app routes and brand assets:

- **Android** — Jetpack Compose, minSdk 26, Kotlin.
- **iOS** — SwiftUI (iOS 16 → 26 via three support tiers), XcodeGen project,
  WidgetKit widget, AppIntents, Spotlight, background refresh.
- **Web** — Nuxt SSG PWA, installable on mobile/desktop and published to
  GitHub Pages by `.github/workflows/web-pages.yml` from `main`.

> ⚠️ **Building this project? Read [`AGENTS.md`](AGENTS.md) first — all builds
> and tests run on GitHub Actions only, never locally.**

## What's inside

| Area | Detail |
|---|---|
| **Home** | Mission hero, upcoming-events carousel, latest stories, impact bento, action tiles |
| **Events** | Live feed from the site's calendar, native detail screens with "Save to calendar" (EventKit on iOS) |
| **Stories** | The Earth Guardian blog with search + a Vue 3 + VueUse reading experience |
| **EG-Maps** | Atlas, regional map and endangered-species pages of the site |
| **More** | Full sitemap-driven navigation, notifications center, settings |
| **Web PWA** | Static Home, Events, Stories, Maps & tools, More, Settings and Updates screens; cached feed data and an offline shell |

- **Design:** black/white + iOS red `#FF3B30` / green `#34C759`, glassmorphism,
  bento layouts, System/Light/Dark theme switching.
- **Integration:** native screens for structured content (RSS feeds, curated
  catalog) and a themed in-app browser with a dark filter for every page of the
  site — new pages published to the site appear in the app automatically.
- **Platform (Android):** notifications (3 channels + in-app history), periodic
  WorkManager sync, deep links (`earthguardians://…` + website links), app
  shortcuts (Events / Stories / Donate), adaptive launcher icon, splash screen,
  share intents, calendar intents, Android backup rules.
- **Platform (iOS):** the same feature set natively — WidgetKit "Upcoming
  Events" widget, AppIntents (Siri/Shortcuts), CoreSpotlight site indexing,
  BGTaskScheduler 6h sync, EventKit calendar, share sheets, URL scheme,
  webpage-route handling and notification taps.
- **Web PWA:** deep-link arguments (`?page=events`, `?page=section&u=…` or
  `?route=earthguardians://…`), install manifest, service worker and static
  routes suitable for GitHub Pages. The in-app mark and PWA icons are generated
  with Pillow from the same official master used by Android and iOS.

## iOS support tiers

CI produces **three unsigned IPAs**, one per support range — the newest tier
uses the newest SDK and UI kit, older tiers keep maximum device compatibility:

| Artifact | For devices on | Built with | UI kit |
|---|---|---|---|
| `ios-ipa-iOS16` | iOS 16 – 17 | Xcode 16 (`macos-15`) | Hand-painted glass (the iOS 16 floor) |
| `ios-ipa-iOS18` | iOS 18 – 25 | Xcode 16 (`macos-15`) | + `EG_MODERN`: ContentUnavailableView, symbol effects, aligned carousels |
| `ios-ipa-iOS26` | iOS 26+ | Xcode 26 (`macos-26`) | + `EG_LATEST`: Liquid Glass surfaces & glass buttons |

## Architecture at a glance

- Single activity (Android) / single App scene (iOS), no DI framework —
  `AppGraph` + `LocalAppGraph` / `AppGraph.shared`.
- Data: Squarespace RSS + `/sitemap.xml` → OkHttp / async URLSession →
  disk-cached JSON → StateFlow / Combine publishers.
- Site pages open in `WebViewScreen` / `SiteWebView` (themed, with an optional
  invert/hue-rotate dark filter). Story reading happens in a bundled Vue +
  VueUse reader shell (WKWebView + JS bridge on iOS).
- Pure logic is testable without a device: JVM tests (`app/src/test/…`) and the
  SwiftPM package `ios/Packages/EGCore` (`swift test` on CI).
- Architecture document: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) —
  includes the full sitemap → native vs WebView mapping table and the iOS map.

## Requirements

- Android 8.0+ device (minSdk 26) · iPhone/iPad on iOS 16+ (per-tier IPAs)
- Nothing else — contributors do **not** need a local Android or Xcode
  toolchain (see [`AGENTS.md`](AGENTS.md)).

## Building

| Artifact | How it's produced |
|---|---|
| Universal APK + AAB | `ci.yml` on every push/PR → artifacts |
| iOS IPA (three tiers) | `ci.yml` (`ios-ipa-iOS16/18/26` artifacts) |
| Unit tests | `ci.yml` (JVM + `swift test`) |
| Static web PWA | `web-pages.yml` on PR (build verification) and `main` (deploy to Pages) |
| Release build (APK + AAB + 3 IPAs) | `release.yml` on tag `v*` |

For the first Pages deployment, set the repository's Pages source to **GitHub
Actions**. Project Pages builds use `/<repository-name>/` automatically; if a
custom domain serves the app from `/`, define the Actions variable
`NUXT_APP_BASE_URL` as `/`.

```bash
# after your PR is green on CI:
gh run download <run-id> -n android-universal-apk
gh run download <run-id> -n ios-ipa-iOS26
```

## For the organization

- Contact: reachout@earthguardians.org · PO Box 1561, Laurel, MD 20725 ·
  EIN 84-1397083
- 100% youth-led. 94% youth & 88% BIPOC staff. 1M+ people impacted.
  20K+ trees planted across Africa.
