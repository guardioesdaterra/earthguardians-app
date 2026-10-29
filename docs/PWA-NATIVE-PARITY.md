# PWA and native UI parity review

## Current status

This isolated PWA branch implements the requested alignment from the supplied `main` base. It does **not** claim complete pixel or flow parity: no runtime screenshots or full user-flow comparisons have been run. Local builds/tests have not been run, and nothing has been pushed or merged.

## Implemented contract

| Flow | Native reference | PWA implementation and static evidence |
| --- | --- | --- |
| App shell | Android `app/src/main/kotlin/org/earthguardians/app/ui/root/EgAppRoot.kt`; iOS `ios/EarthGuardians/UI/Root/RootView.swift` | `web/layouts/default.vue`: five tabs, Home header actions, detail navigation behavior and unread notification count. `web/assets/css/main.css`: native light/dark palette, tab bar and safe-area spacing. `web/app.vue` owns one theme listener and starts feed refresh on every route. |
| Home | Android and iOS `HomeScreen` | `web/components/HomePage.vue`: mission copy, approved local Benin image hero, events carousel, Stories rows, impact stats and CTA destinations. Home labels follow Android's current screen text; iOS uses more descriptive `SiteCatalog.Impact` labels for the same figures. Approved Home images are listed in `docs/official-home-image-sources.md`. |
| First run | Android and iOS `OnboardingScreen` | `web/components/OnboardingGate.vue`: four native headings/copy/features, swipe/dot navigation, skip and a default-on but optional alerts toggle. Permission is requested only after the explicit Get started action; a swipe cannot trigger it. The UI explains HTTPS and iPhone/iPad installation requirements first. Skip opts out. |
| Events | Android/iOS event lists and native event detail | `web/components/EventsPage.vue`, `EventCard.vue`, `QueryDestination.vue`: searchable native-style rows, detail route, share and calendar export. Calendar export uses a two-hour fallback, escapes ICS text/URL fields, and retains the Blob URL for mobile download handling. |
| Stories | Android/iOS story list and reader | `web/components/StoriesPage.vue`, `StoryCard.vue`, `QueryDestination.vue`: list cards route to the full feed HTML reader; fetched HTML is sanitized before display. |
| Feed lifecycle | Native loading, failure/retry and stale/offline states | `web/composables/useEgFeed.ts`, `FeedStatus.vue`, and the Home/Events/Stories pages: cached content is marked stale until a live release snapshot succeeds; refresh runs on every app route, loading/error/retry/stale UI is available, and the first sync silently records the complete current snapshot. Later refreshes keep every currently present key plus up to 500 previously seen items no longer in the snapshot, preventing an archived feed larger than 500 items from generating alerts on the next sync. Browser alerts occur only while the PWA is open and checking feeds; no background delivery is claimed. |
| Maps | Shared contract confirmed by RICK; Android MapLibre screen is the tie-breaker for the web/native baseline | `web/components/MapsPage.vue` and `MapLibreCanvas.vue`: initial center `(15, 0)` and approximately `220°` longitude span; region aggregates at visible span `>=45°`, individual Crew markers below; status-colored Crew markers (Active green, Inactive amber) and a matching legend; region details with active/inactive Crew counts, members and countries; Crew details with name, city/state/country, region and status; Project details with title, `country_province` and direct/indirect beneficiaries. Search covers Crew name/city/state/country/region/status, region name/id, and Project title/location, with a clear action and filtered markers. Android/iOS source arrays match: 7 regions, 169 Crews, 72 grants. `web/scripts/sync-map-assets.mjs` checks Android/iOS data drift before copying the snapshots to the PWA. |
| More | Android/iOS `SiteCatalog` and More screens | `web/components/MorePage.vue` mirrors curated groups and lists up to 40 top-level pages from the bundled sitemap snapshot. It describes these as selected pages rather than claiming the entire site. The manager-only `eg-grants` portal is not linked. |
| Settings and inbox | Android/iOS Settings and Notifications screens | `web/components/SettingsPage.vue` has Automatic/Light/Dark, optional browser alerts, a test alert and About/contact rows. `web/components/NotificationsPage.vue` supports opening an update, unread state, mark-all-read and clear. Home and More display unread counts. Permission copy covers HTTPS and the iPhone/iPad installed-PWA requirement; background refresh is unavailable on the web. |

## Map renderer and attribution exception

The PWA uses MapLibre GL JS with the OpenFreeMap Liberty style; its MapLibre attribution control is retained for the basemap credits, including OpenMapTiles and OpenStreetMap. Android also uses MapLibre and identifies OpenMapTiles/OpenStreetMap in its map UI. iOS still uses Apple MapKit and credits Apple map data. Its renderer, basemap and attribution are therefore an explicit exception until iOS moves to a common renderer or visual/attribution parity is demonstrated. **No pixel-parity claim is made while this difference remains.**

The PWA follows the RICK-confirmed shared behavior contract. Android's checked-in implementation currently selects its region level by zoom (`4.5`); the PWA uses the contract's viewport longitude span threshold (`45°`) so the behavior is tied to visible coverage rather than a device-dependent zoom value. This Home follow-up changes local thumbnail selection on native and web surfaces but does not change the map contract.

## Unresolved local-imagery scope

The three featured Home thumbnails shown in the comparison are now bundled locally with matching bytes across Android, iOS and PWA. The Home selects them by canonical Earth Guardians event/story path, so the deterministic fixture can show the approved images even though it sets `imageUrl` to null. This resolves only those three records: other dynamic feed thumbnails and images inside story HTML may still come from the external Squarespace CDN. Local map records are bundled, but basemap style/tiles are remote and still require internet access. **Complete offline delivery for feed imagery and map tiles remains unresolved.**

## Verification boundary

Static source review confirms the map data fields, filtering/detail contracts, status colors/legend, initial camera and span-based aggregation logic, and attribution/offline notes. Android and iOS map JSON arrays are identical; the sync script checks for future drift. The Home hero and three mapped feed thumbnails are approved local images; other feed content and basemap tiles remain outside that coverage.

No local build, test, or new screenshot comparison has been run for this follow-up. The visual changes are therefore **not verified** and the CI comparator must pass on the integrated commit before merge. The MapKit renderer/attribution exception is documented above.
