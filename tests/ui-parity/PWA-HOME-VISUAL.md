# PWA Home visual comparison

This is a **masked screenshot regression check**, not a claim of complete pixel parity. It compares the integrated PWA Home with the fixed Android Home capture from Actions run `40876025810`.

## Reference crop and PWA viewport

The Android source is `tests/fixtures/pwa-home/android-home-run-40876025810.png` (1080×2400). The comparator takes the content rectangle `[left=0, top=149, right=1080, bottom=2336)`, yielding 1080×2187 px. This excludes the Pixel 6 status/camera inset above the first Home content and the gesture-navigation inset below the app; the Android bottom bar remains in the compared content.

The PWA capture uses Chromium in light mode, `en-US`, UTC, a 412×834 CSS viewport and device scale factor 2.625. The desired content extent is 411.4286×833.1429 CSS px, but a fractional Playwright clip can round to 1079 px wide. To avoid that ambiguity, CI captures the full viewport and crops the PNG from its top-left origin to exactly 1080×2187 device pixels, without resizing. The PWA has no Android system bars, so the screenshot starts at its app content edge.

## Deterministic feed and masks

Before static generation, CI temporarily substitutes `tests/fixtures/pwa-home/feed.json` for the imported feed cache. The browser also intercepts the feed refresh request with that same fixture. Event/story titles and dates are therefore fixed, images are omitted, and the browser uses a fixed locale and timezone.

The capture script discovers only the visible `.home-event-art` rectangles and `.home-story-row` thumbnails. Those physical-pixel rectangles are masked in **both** screenshots to tolerate live/remote event artwork, story thumbnails and the overlaid event date. The mask may cover at most 30% of the crop. Section headings, event/story text, app shell, navigation, and the Home hero are not masked.

The current parity source uses a photographic Home hero and different hero copy from the Android quote card. That known visual difference remains unmasked so the comparison can expose it; this check does not silently label that surface as matched.

## Thresholds and CI gate

The comparator passes only when both conditions hold over unmasked pixels:

- mean absolute RGB error ≤ 12.0 (0–255 channel scale);
- at most 12% of pixels have a maximum RGB-channel difference greater than 48.

The `PWA Home visual` workflow first checks for all integrated PWA markers: `web/components/HomePage.vue`, `web/assets/css/main.css`, `web/package-lock.json`, and `web/cache/eg_feed_cache.json`. It skips the screenshot job when any is absent, so the current native-only `main` does not receive a misleading visual failure. Once those files are present in a PR or on `main`, the job generates and captures the PWA in GitHub Actions and enforces the thresholds. It uploads the PWA image, native crop, mask JSON and diff image even when comparison fails.

No local build or test was run for this change. The first enabled Actions run is the verification surface for the viewport rasterization and threshold behavior.
