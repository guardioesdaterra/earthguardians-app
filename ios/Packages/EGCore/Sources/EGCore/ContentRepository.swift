import Foundation
import Combine

/**
 * Single content surface for the whole app: events feed, blog feed and the live
 * sitemap of earthguardians.org. Disk-cached for offline-first behavior and
 * refreshed through URLSession on demand and by the periodic background sync.
 */
public final class ContentRepository: ObservableObject {

    @Published public private(set) var feed: FeedState = .loading
    @Published public private(set) var sitemap: SitemapState = .loading

    private let session: URLSession
    private let cacheDirectory: URL
    private var lastFeedSyncAt: TimeInterval = 0
    private var lastSitemapSyncAt: TimeInterval = 0
    private var feedSyncLock = NSLock()
    private var sitemapSyncLock = NSLock()

    /// Hook the notification coordinator sets: invoked after every successful sync.
    public var onFeedSynced: ((EgFeed) -> Void)?

    public init(
        session: URLSession = {
            let config = URLSessionConfiguration.ephemeral
            config.timeoutIntervalForRequest = 15
            config.timeoutIntervalForResource = 45
            config.httpAdditionalHeaders = ["User-Agent": "EG-App/1.0 (iOS; +https://www.earthguardians.org)"]
            return URLSession(configuration: config)
        }(),
        cacheDirectory: URL? = nil
    ) {
        self.session = session
        self.cacheDirectory = cacheDirectory
            ?? FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        self.cacheDirectory = self.cacheDirectory.appendingPathComponent("eg_content", isDirectory: true)
        try? FileManager.default.createDirectory(at: self.cacheDirectory, withIntermediateDirectories: true)

        Task { [weak self] in
            await self?.restoreCache()
            try? await Task.sleep(nanoseconds: 250_000_000) // let splash/onboarding breathe
            await self?.refreshFeed()
            await self?.refreshSitemap()
        }
    }

    // MARK: - Refresh

    /// Refreshes the events + blog feeds (force to bypass the 30-min TTL).
    @discardableResult
    public func refreshFeed(force: Bool = false) async -> Void {
        guard feedSyncLock.withLock({ () -> Bool in
            if !force, lastFeedSyncAt > 0, Date().timeIntervalSince1970 - lastFeedSyncAt < Self.feedTTL { return false }
            return true
        }) else { return }

        async let posts = Self.mapPosts(RssParser.parse(await fetchTextAsync(SitePaths.blogFeedRSS)))
        async let events = Self.mapEvents(RssParser.parse(await fetchTextAsync(SitePaths.eventsFeedRSS)))
        let (mappedPosts, mappedEvents) = await (posts, events)

        // Keep the last good feed through transient hiccups.
        if mappedPosts.isEmpty && mappedEvents.isEmpty && lastFeedSyncAt > 0 { return }

        let result = EgFeed(events: mappedEvents, posts: mappedPosts)
        await MainActor.run {
            feed = .ready(result, stale: false)
        }
        lastFeedSyncAt = Date().timeIntervalSince1970
        writeCache(FeedCache(fetchedAt: Date(), feed: result), name: "eg_feed_cache.json")
        onFeedSynced?(result)
    }

    /// Refreshes the sitemap (force to bypass the 6-hour TTL).
    @discardableResult
    public func refreshSitemap(force: Bool = false) async -> Void {
        guard sitemapSyncLock.withLock({ () -> Bool in
            if !force, lastSitemapSyncAt > 0, Date().timeIntervalSince1970 - lastSitemapSyncAt < Self.sitemapTTL { return false }
            return true
        }) else { return }

        let urls = SitemapParser.parse(await fetchTextAsync(SitePaths.sitemapURL))
        guard !urls.isEmpty else {
            await MainActor.run {
                if case .loading = sitemap {
                    sitemap = .failed("Sitemap unreachable — showing cached pages only.")
                }
            }
            return
        }
        await MainActor.run { sitemap = .ready(urls, stale: false) }
        lastSitemapSyncAt = Date().timeIntervalSince1970
        writeCache(SitemapCache(fetchedAt: Date(), urls: urls), name: "eg_sitemap_cache.json")
    }

    /// Direct async URL fetch (non-blocking on cooperative threads).
    public func fetchTextAsync(_ urlString: String) async -> String {
        guard let url = URL(string: urlString) else { return "" }
        return await withCheckedContinuation { continuation in
            session.dataTask(with: url) { data, response, _ in
                guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
                      let data = data, let body = String(data: data, encoding: .utf8) else {
                    continuation.resume(returning: "")
                    return
                }
                continuation.resume(returning: body)
            }.resume()
        }
    }

    /// Blocking variant for non-async call sites (widget timeline provider).
    public func fetchText(_ urlString: String) -> String {
        if Thread.isMainThread { return Self.blockingSemaphoreFetchWarning() ?? "" }
        var payload = ""
        let semaphore = DispatchSemaphore(value: 0)
        let task = session.dataTask(with: URL(string: urlString) ?? URL(fileURLWithPath: "/")) { data, response, _ in
            defer { semaphore.signal() }
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
                  let data = data, let body = String(data: data, encoding: .utf8) else { return }
            payload = body
        }
        task.resume()
        _ = semaphore.wait(timeout: .now() + 50)
        return payload
    }

    /// Sync network access is not allowed on the main thread — keeps call sites honest.
    private static func blockingSemaphoreFetchWarning() -> String? {
        assertionFailure("fetchText must not be called on the main thread; use fetchTextAsync")
        return nil
    }

    // MARK: - Mapping RSS → domain

    static func mapPosts(_ items: [RssParser.RssItem]) -> [EgPost] {
        items.compactMap { item in
            guard let link = item.link, link.contains("/earthguardian-blog/") else { return nil }
            let title = (item.title?.isEmpty == false ? item.title : nil)
                ?? EgText.titleFromSlug(EgText.slugOf(link))
            let teaser = EgText.fromHtml(item.description)
            return EgPost(
                url: link,
                title: title,
                imageUrl: item.imageUrl,
                publishedAt: RssParser.parsePubDate(item.pubDate),
                teaser: teaser.isEmpty ? nil : teaser,
                contentHtml: item.contentHtml?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfBlank
            )
        }
    }

    static func mapEvents(_ items: [RssParser.RssItem]) -> [EgEvent] {
        items.compactMap { item in
            guard let link = item.link, link.contains("/events/") else { return nil }
            let title = (item.title?.isEmpty == false ? item.title : nil)
                ?? EgText.titleFromSlug(EgText.slugOf(link))
            let description = EgText.fromHtml(item.description)
            return EgEvent(
                url: link,
                title: title,
                imageUrl: item.imageUrl,
                startsAt: RssParser.parsePubDate(item.pubDate) ?? dateFromEventSlug(link),
                location: item.category,
                description: description.isEmpty ? nil : description
            )
        }
    }

    /// Event URLs look like /events/2025/6/14/project-grant-summit — parse the date.
    static func dateFromEventSlug(_ url: String) -> Date? {
        let noQuery = String(url.split(separator: "?", maxSplits: 1).first ?? Substring())
        let segments = noQuery.split(separator: "/").map(String.init)
        guard segments.count >= 4, let year = Int(segments[1]), let month = Int(segments[2]), let day = Int(segments[3]) else { return nil }
        var components = DateComponents()
        components.year = year; components.month = month; components.day = day
        return Calendar(identifier: .gregorianUTCFallback).date(from: components)
    }

    // MARK: - JSON disk cache (offline-first)

    private func restoreCache() async {
        if let feedCache: FeedCache = readCache(name: "eg_feed_cache.json"), !feedCache.feed.posts.isEmpty || !feedCache.feed.events.isEmpty {
            await MainActor.run { feed = .ready(feedCache.feed, stale: true) }
            lastFeedSyncAt = 0 // allow an immediate refresh attempt
        }
        if let sitemapCache: SitemapCache = readCache(name: "eg_sitemap_cache.json"), !sitemapCache.urls.isEmpty {
            await MainActor.run { sitemap = .ready(sitemapCache.urls, stale: true) }
            lastSitemapSyncAt = 0
        }
    }

    private func writeCache<T: Encodable>(_ value: T, name: String) {
        guard let json = try? JSONEncoder().encode(value) else { return }
        try? json.write(to: cacheDirectory.appendingPathComponent(name), options: .atomic)
    }

    private func readCache<T: Decodable>(name: String, as type: T.Type = T.self) -> T? {
        guard let data = try? Data(contentsOf: cacheDirectory.appendingPathComponent(name)) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    private static let feedTTL: TimeInterval = 30 * 60      // 30 minutes
    private static let sitemapTTL: TimeInterval = 6 * 3600  // 6 hours
}

extension Optional where Wrapped == String {
    var nilIfBlank: String? {
        guard let value = self else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}

extension Calendar {
    /// Gregorian calendar pinned to UTC so event slug dates are stable everywhere.
    static let gregorianUTCFallback: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .current
        return calendar
    }()
}

extension NSLock {
    func withLock<T>(_ body: () -> T) -> T {
        lock()
        defer { unlock() }
        return body()
    }
}
