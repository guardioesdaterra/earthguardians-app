import Foundation

/// URL roots of the Earth Guardians ecosystem.
public enum SitePaths {
    public static let siteURL = "https://www.earthguardians.org"
    public static let sitemapURL = "\(siteURL)/sitemap.xml"

    /// Squarespace exposes RSS per collection page.
    public static let blogFeedRSS = "\(siteURL)/earthguardian-blog?format=RSS"
    public static let eventsFeedRSS = "\(siteURL)/events?format=RSS"
}

/// One upcoming or past event from the EG events feed.
public struct EgEvent: Codable, Hashable, Identifiable {
    public var id: String { url }
    public let url: String
    public let title: String
    public let imageUrl: String?
    public let startsAt: Date?
    public let location: String?
    public let description: String?
}

/// One story/blog post from the Earth Guardian blog.
public struct EgPost: Codable, Hashable, Identifiable {
    public var id: String { url }
    public let url: String
    public let title: String
    public let imageUrl: String?
    public let publishedAt: Date?
    public let teaser: String?
    public let contentHtml: String?
}

public struct EgFeed: Codable, Hashable {
    public var events: [EgEvent]
    public var posts: [EgPost]

    public init(events: [EgEvent] = [], posts: [EgPost] = []) {
        self.events = events
        self.posts = posts
    }
}

/// Disk cache wrapper (offline-first).
struct FeedCache: Codable {
    var fetchedAt: Date
    var feed: EgFeed
}

struct SitemapCache: Codable {
    var fetchedAt: Date
    var urls: [String]
}

/// UI state of the feeds — loading → ready (possibly stale) | failed.
public enum FeedState {
    case loading
    case ready(EgFeed, stale: Bool)
    case failed(String)
}

/// UI state of the live sitemap — loading → ready (possibly stale) | failed.
public enum SitemapState {
    case loading
    case ready([String], stale: Bool)
    case failed(String)
}

/// Curated accent for catalog tiles — red = action, green = nature, neutral = information.
public enum SectionAccent: String, Codable, Hashable {
    case neutral, red, green
}

/// Which section of the iOS-style bento the catalog entry belongs to.
public enum SectionGroup: String, Codable, CaseIterable, Hashable {
    case about, people, involve, explore, resources, utility

    public var title: String {
        switch self {
        case .about: return "Who We Are"
        case .people: return "Our People"
        case .involve: return "Get Involved"
        case .explore: return "Programs & Maps"
        case .resources: return "Resources"
        case .utility: return "More"
        }
    }
}

/// A curated entry of the earthguardians.org site, integrated into the app.
public struct SiteSection: Hashable, Identifiable {
    public var id: String { path }
    public let title: String
    public let subtitle: String
    public let path: String
    public let group: SectionGroup
    public let accent: SectionAccent

    public var url: String { "\(SitePaths.siteURL)/\(path)" }
}
