# EarthGuardians App — Architecture

The app integrates **all** of [earthguardians.org](https://www.earthguardians.org),
discovered from the live [`/sitemap.xml`](https://www.earthguardians.org/sitemap.xml)
(≈150 URLs at integration time). Where a page's content is structured, it is
rendered natively with full theming; the remaining pages open in a themed
in-app browser. Content published to the site later surfaces automatically via
the dynamic sitemap provider.

```
┌────────────────────────── MainActivity ──────────────────────────┐
│  EGApp (Application: ConfigProvider for WorkManager)             │
│  └ EgAppRoot ── onboarding gate ── NavHost + glass bottom bar    │
│      ├ HomeScreen      (native)     ├ EventsScreen     (native)  │
│      ├ EventDetail     (native)     ├ StoriesScreen     (native)  │
│      ├ StoryReader     (Vue shell)  ├ MapsScreen        (native)  │
│      ├ MoreScreen      (native)     ├ SettingsScreen    (native)  │
│      ├ Notifications   (native)     ├ WebViewScreen     (themed)  │
│      └ RouteBus ◄── deeplinks · shortcuts · notification taps     │
└───────────────────────────────────────────────────────────────────┘
```

## Composition root (no DI framework)

`graph/AppGraph.kt` is a hand-wired singleton graph provided through the
`LocalAppGraph` CompositionLocal:

| Member | Responsibility |
|---|---|
| `theme: ThemeRepository` | System/Light/Dark mode via DataStore; single palette source |
| `prefs: PrefsRepository` | Onboarding flag, notification history (kotlinx-serialization) |
| `content: ContentRepository` | RSS + sitemap fetching, disk-cached offline-first feeds |
| `notifications: NotificationCoordinator` | 3 channels, push, test notification, history |
| `routeBus: RouteBus` | Pending external routes (deeplinks/shortcuts/notification taps) |
| `scope` | App-lifetime coroutine scope for OkHttp + WorkManager plumbing |

## Data pipeline

```
Squarespace RSS (per-collection ?format=RSS)  ─┐
  /earthguardian-blog?format=RSS               ├► XmlPullParser ─► EgFeed
  /events?format=RSS                           ┘
https://www.earthguardians.org/sitemap.xml ────► SitemapParser ─► List<URL>
        │                                              │
        ▼ OkHttp (background scope)                    ▼
   JSON disk cache (cacheDir, stale-aware)   SiteCatalog (curated) + dynamic merge
        │
        ▼ StateFlow<FeedUiState / SitemapUiState>
   Loading · Ready(data, isStale) · Failed(message)   ← offline-first: cached
                                                       snapshot always wins
```

- Parsers are namespace-agnostic (`content:encoded`, `media:thumbnail`,
  `media:content`, `enclosure` matched by local name) and pure JVM —
  unit-tested with JUnit 4 + `xmlpull`/`kxml2`, no Robolectric, no device.
- Failed network never clears the cache: feeds surface `Ready(isStale = true)`
  with an "offline · showing cached content" pill.

## Sitemap integration map

Decision rule: **native** when the page is a structured collection (feed) or a
curated function-rich hub; **themed WebView** for everything else — free-form
Squarespace pages that would lose their editorial layout and embeds if
hand-translated to Compose.

### Native screens

| Destination | Source | Why native |
|---|---|---|
| Home | Blog + events feeds, `SiteCatalog`, impact report stats | Editorial hub, bento layout, app-specific composition |
| Events tab | `/events` + `/events?format=RSS` | 24 real event pages; feed gives titles, dates, images |
| Event detail | Event feed item | Native actions: calendar intent, share |
| Stories tab | `/earthguardian-blog` + RSS | 123+ posts with search, sorted by pubDate |
| Story reader | Post JSON → `assets/eg_story/index.html` | Bundled Vue 3 + @vueuse/core reader (progress, font size, swipe-close) driven through the `EGAndroid` JS bridge |
| EG-Maps tab | `/eg-maps`, `/endangered-species-map`, `/eg-programs` | The site's map pages are fully JS-rendered Squarespace blocks (empty in static fetch) — native hub + curated tabs, pages open in the themed browser |
| More | Dynamic sitemap + `SiteCatalog` | Full site surface, grouped: About / Involve / Programs & Maps / Resources / Utility |
| Settings, Notifications | App-local | Theme mode, channels, per-category push, history |

### Curated family destinations (themed WebView)

`SiteCatalog` carries the real site paths — every entry verified live (Oct 2026),
including the "Our People" director/speaker profile pages:

- **Who We Are** — `our-story`, `eg-team`, `eg-impact`, `founders`,
  `board-of-directors`, `jobs`
- **Our People** — `catherine-mongella-speakersbureau`, `tyler-massias`,
  `marlow`, `staff-anpao-locke`, `tony-perez-soto`, `sebi`, `moudjbath`,
  `rejoice-speakersbureau`, plus the Youth Council profiles
  `youth-councils/leala-pourier-speakersbureau`, `youth-councils/nina-berglund`,
  `youth-councils/maya-lazzaro`
- **Get Involved** — `crews`, `crews-2`, `crews-sign-up-1`, `take-action`,
  `donate`, `become-a-member-1`, `speakers-bureau`, `booking`,
  `speakers-bureau-training`, `can`, `regional-crew-council`,
  `bipoc-leadership`, `indigenous-youth-initiative`,
  `youth-leadership-training`, `leadership-training`, `campaigns-2`, `stmp`,
  `civic-engagement`, `divest-defund`, `inspire`, `birthday-pledge`,
  `jointherevolution`
- **Programs & Maps** — `eg-maps`, `endangered-species-map`, `eg-grants`,
  `eg-programs`
- **Resources** — `eg-resources`, `eg-projectguides-toolkits`, `project-grants`,
  `sdgs`, `principles-of-environmental-justice`, `decolonization-social-equity`,
  `dictionary-of-important-terms`, `sustainable-solutions-waste-reduction`,
  `recipe-contest`
- **More** — `contact`, `subscribe`, `thank-you-for-becoming-a-giver`,
  `privacy-policy`

Any sitemap URL **not** in the catalog still opens in the themed browser
(`Routes.section`), so the app can never render a dead end for new pages.

## Themed browser (`ui/web/WebViewScreen.kt`)

- WebView session shared, theme palette applied natively around the chrome;
  optional user-toggled dark filter (`invert(1) hue-rotate(180deg)` JS/CSS)
  because Squarespace is light-only.
- `EgWebBridge` (`window.EGAndroid`) exposes close/open/share to embedded shells.
- External navigation stays in-app; http(s) links to the site re-compress into
  `Routes.section`.

## The Vue reader shell

`app/src/main/assets/eg_story/index.html` — Vue 3.5 + @vueuse/core (via esm.sh
import map): `useTitle`, `useStorage` (font-size persistence), `useEventListener`,
`useSwipe` (swipe-right to close), `useDebounceFn`, `whenever`. The hosting
Compose screen delivers the story payload with a **retrying** `EGStory.load`
call since esm.sh modules resolve after `onPageFinished`. Escapes back via
bridge: close → `popBackStack`, open → section route, share → chooser intent.

## Notifications & background sync

- Channels: events, stories, general (`NotificationCoordinator`).
- `SyncWorker` (WorkManager, 6h periodic) refreshes both feeds; new items
  compared against persisted history produce system notifications.
- `EGApp` is a Configuration.Provider with the WorkManager default initializer
  removed in the manifest.

## Deep links & entry points

Handled by pure-Kotlin `Deeplinks.parseRoute` in `graph/AppGraph.kt`
(unit-tested), consumed through `RouteBus`:

- `earthguardians://home | events | stories | maps | more`
- `earthguardians://event?e=…` · `post?u=…` · `section?u=…`
  (values are decoded-then-encoded once — shortcuts ship pre-encoded URLs)
- `https://(www.)earthguardians.org/<any page>` → themed in-app browser
- App shortcuts: Events, Stories, Donate (`xml/shortcuts.xml`)

## Version platform

Kotlin 2.1.0 · Compose BOM 2025.01.00 · AGP 8.7.3 · Gradle 8.11.1 ·
 JDK 17 · minSdk 26 · target/compile SDK 35 · OkHttp 4.12 · Coil 2.7 ·
navigation-compose 2.8.5 · WorkManager 2.10 · DataStore 1.1.1

---

# iOS app (SwiftUI twin)

Same product surface, native SwiftUI. Deployment targets come in **three
tiers**, each archived into its own unsigned IPA by CI (see the table in
`AGENTS.md` / `README.md`):

```
┌────────────────────────── EarthGuardiansApp (@main) ─────────────────────────┐
│  EGAppDelegate — BGTask registration · quick actions · Spotlight ·           │
│                  notification taps (UNUserNotificationCenterDelegate)         │
│  └ RootView ── onboarding gate ── TabView + floating glass bottom bar         │
│      ├ HomeScreen        (EgImage/AsyncImage)   ├ EventsScreen                │
│      ├ EventDetailScreen (EventKit + share)     ├ StoriesScreen + StoryReader │
│      ├ MapsScreen        (Atlas/Regions/Species) ├ MoreScreen (sitemap index) │
│      ├ SettingsScreen    (theme switch)         ├ NotificationsScreen         │
│      └ SiteWebView       (themed WKWebView)     └ RouteBus ◄ deeplinks ·      │
│                                             shortcuts · intents · Spotlight  │
└───────────────────────────────────────────────────────────────────────────────┘
```

- **Composition root:** `AppGraph.shared` (@MainActor) — publishes theme +
  onboarding mirrors; `RouteBus` funnels every external arrival (deep links,
  notifications, quick actions, App Intents, Spotlight) into the root view.
- **Project:** generated on CI by XcodeGen from `ios/project.yml`
  (never committed). App target + `EGWidget` WidgetKit extension, both linking
  the SwiftPM package `EGCore`.
- **EGCore (pure Foundation, `swift test` on CI):** Models, `RssParser`
  (namespace-agnostic XMLParser), `SitemapParser`, `SiteCatalog`, `Deeplinks`,
  `EgText`, `ContentRepository` (async URLSession + JSON disk cache).
- **Story reader:** the same Vue shell, loaded from `Bundle.main`
  (`Resources/index.html`); `window.EGAndroid.{close,open,share}` is shimmed at
  `documentStart` onto `WKScriptMessageHandler`; payload delivery retries until
  `window.EGStory` exists (esm.sh hydration lag), mirroring Android.
- **Site browser:** `SiteWebView` — KVO on `estimatedProgress` for the progress
  bar, coordinator-owned `goBack()` (web history first, pop on first page),
  per-navigation dark filter, external hosts handed to Safari.
- **Integrations:** WidgetKit timeline (events RSS → 6h refresh → `earthguardians://events`),
  AppIntents (`OpenEvents`, `OpenStories`, `DonateNow` + AppShortcutsProvider),
  CoreSpotlight indexing of the whole curated catalog, EventKit edit sheet,
  `UNUserNotificationCenter` with three categories mirroring the Android
  channels, BGAppRefreshTask (`org.earthguardians.app.sync`).

## Tier plumbing (three kits, one source)

`UI/Design/KitTiers.swift` compiles differently per build configuration
(flags wired in `ios/project.yml`):

- `Release` — iOS 16.0 floor: hand-painted glass (fill + hairline stroke),
  no tier flags.
- `Release-iOS18` — `EG_MODERN`: `ContentUnavailableView` states, symbol
  bounce effects, view-aligned carousel targets, two-parameter `onChange`.
- `Release-iOS26` — `EG_MODERN` + `EG_LATEST`: Liquid Glass (`glassEffect`,
  `.buttonStyle(.glass)`) on chrome, built with the iOS 26 SDK on `macos-26`.

All other source stays on the iOS 16 floor so every tier compiles from the
same tree; CI archives each configuration into `EG-iOS16/18/26.ipa`.
