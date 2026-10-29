# Resource Library — data contract

The native resource slice is a metadata-only index in Android's `data/model` + `data/repo` packages and iOS `EGCore`. It does not add screens, routes, network scraping, or bundled source material.

Each record has a stable ID, short label, category, canonical destination, source page, review timestamp, review interval, freshness hint, and optional search labels. Search labels are identifiers/topics only; summaries, page text, PDFs, images, and third-party assets are not copied into the app.

## Links and source freshness

- `canonicalUrl` / `canonicalURL` is the destination the app may open.
- `sourceUrl` / `sourceURL` identifies the official page supporting the record.
- `CollectionFallback` means research confirmed a resource from a parent collection, but not its exact PDF, Drive, Docs, YouTube, or external destination. Keep the canonical URL on that verified collection; do not guess a direct link.
- `reviewedAtEpochMillis` records the review date in UTC. The 180-day interval is a maintenance reminder, not a promise that the source is current for 180 days.
- A source hint can force review for pages known to contain historical dates. `opportunityEndsAtEpochMillis` expires only the dated opportunity, not the underlying program page.
- Guide difficulty levels 1–3 and time estimates are recorded at the collection level. Do not infer a specific guide's level or duration until its source is checked.

When adding an item, verify its destination and supporting source page, record an explicit fallback if its exact external link is unresolved, and set a review hint for stale or date-sensitive information. Keep Kotlin and Swift catalogs in the same order and with matching IDs/metadata.

The unit tests for this core run in GitHub Actions only, as required by `AGENTS.md`; do not run local builds or tests.
