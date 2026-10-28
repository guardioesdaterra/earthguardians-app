# EarthGuardians — Official App

The official Android app of [EarthGuardians.org](https://www.earthguardians.org)
— a youth-led, BIPOC-centered climate justice organization ("You can be the
single drop of water that creates an enormous ripple effect" — Emmy Scott,
Executive Director).

Native, iOS-styled Android (Jetpack Compose), fully themed, offline-first, and
integrating the entire site from `earthguardians.org/sitemap.xml`.

> ⚠️ **Building this project? Read [`AGENTS.md`](AGENTS.md) first — all builds
> and tests run on GitHub Actions only, never locally.**

## What's inside

| Area | Detail |
|---|---|
| **Home** | Mission hero, upcoming-events carousel, latest stories, impact bento, action tiles |
| **Events** | Live feed from the site's calendar, native detail screens with "Save to calendar" |
| **Stories** | The Earth Guardian blog with search + a Vue 3 + VueUse reading experience |
| **EG-Maps** | Atlas, regional map and endangered-species pages of the site |
| **More** | Full sitemap-driven navigation, notifications center, settings |

- **Design:** black/white + iOS red `#FF3B30` / green `#34C759`, glassmorphism,
  bento layouts, System/Light/Dark theme switching.
- **Integration:** native screens for structured content (RSS feeds, curated
  catalog) and a themed in-app browser with a dark filter for every page of the
  site — new pages published to the site appear in the app automatically.
- **Platform:** notifications (3 channels + in-app history), periodic
  WorkManager sync, deep links (`earthguardians://…` + website links), app
  shortcuts (Events / Stories / Donate), adaptive launcher icon, splash screen,
  share intents, calendar intents, Android backup rules.

## Architecture at a glance

- Single activity, Compose-first, no DI framework — `AppGraph` + `LocalAppGraph`.
- Data: Squarespace RSS + `/sitemap.xml` → OkHttp → disk-cached JSON → StateFlow.
- Site pages open in `WebViewScreen` (themed, with an optional invert/hue-rotate
  dark filter). Story reading happens in a bundled Vue + VueUse reader shell.
- Architecture document: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) —
  includes the full sitemap → native vs WebView mapping table.

## Requirements

- Android 8.0+ (minSdk 26)
- Nothing else — contributors do **not** need a local Android toolchain
  (see [`AGENTS.md`](AGENTS.md)).

## Building

| Artifact | How it's produced |
|---|---|
| Debug APK | `ci.yml` on every push/PR → artifact download |
| Release APK | `release.yml` on tag `v*` |
| Unit tests | `ci.yml` (`testDebugUnitTest`) |

```bash
# after your PR is green on CI:
gh run download <run-id> -n earthguardians-apk
```

## For the organization

- Contact: reachout@earthguardians.org · PO Box 1561, Laurel, MD 20725 ·
  EIN 84-1397083
- 100% youth-led. 94% youth & 88% BIPOC staff. 1M+ people impacted.
  20K+ trees planted across Africa.
