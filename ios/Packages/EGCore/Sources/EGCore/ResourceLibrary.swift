import Foundation

public enum ResourceCategory: String, Codable, CaseIterable, Hashable {
    case community
    case funding
    case projectGuides
    case calendar
    case learning
    case campaigns
    case programs
    case reference
}

public enum ResourceLinkScope: String, Codable, Hashable {
    case directPage
    case collectionFallback
}

public enum ResourceFreshnessHint: String, Codable, Hashable {
    case reviewed
    case timeSensitive
    case reviewRecommended
}

public enum ResourceFreshnessStatus: String, Codable, Hashable {
    case current
    case timeSensitive
    case reviewRecommended
    case opportunityExpired
}

/// Provenance for an indexed link. Timestamps are UTC epoch milliseconds. The review interval is
/// a maintenance policy, not a claim that the linked source changes on that schedule.
public struct ResourceSourceMetadata: Codable, Hashable {
    public let canonicalURL: String
    public let sourceURL: String
    public let reviewedAtEpochMillis: Int64
    public let reviewIntervalDays: Int
    public let freshnessHint: ResourceFreshnessHint
    /// Expiry of a specific opportunity mentioned by the source, not of the page itself.
    public let opportunityEndsAtEpochMillis: Int64?

    public init(
        canonicalURL: String,
        sourceURL: String,
        reviewedAtEpochMillis: Int64,
        reviewIntervalDays: Int = 180,
        freshnessHint: ResourceFreshnessHint = .reviewed,
        opportunityEndsAtEpochMillis: Int64? = nil
    ) {
        self.canonicalURL = canonicalURL
        self.sourceURL = sourceURL
        self.reviewedAtEpochMillis = reviewedAtEpochMillis
        self.reviewIntervalDays = reviewIntervalDays
        self.freshnessHint = freshnessHint
        self.opportunityEndsAtEpochMillis = opportunityEndsAtEpochMillis
    }

    public func freshness(atEpochMillis asOfEpochMillis: Int64) -> ResourceFreshnessStatus {
        if let opportunityEndsAtEpochMillis, asOfEpochMillis >= opportunityEndsAtEpochMillis {
            return .opportunityExpired
        }
        if freshnessHint == .reviewRecommended {
            return .reviewRecommended
        }
        let reviewDueAt = reviewedAtEpochMillis + Int64(reviewIntervalDays) * 86_400_000
        if asOfEpochMillis >= reviewDueAt {
            return .reviewRecommended
        }
        if freshnessHint == .timeSensitive {
            return .timeSensitive
        }
        return .current
    }
}

/// Collection-level Choose Action Now facts. Levels and time estimates are not assigned to
/// individual guide topics because those per-guide values were not verified.
public struct ResourceGuideMetadata: Codable, Hashable {
    public let difficultyLevels: [Int]
    public let providesTimeEstimates: Bool

    public init(difficultyLevels: [Int], providesTimeEstimates: Bool) {
        precondition(!difficultyLevels.isEmpty && difficultyLevels.allSatisfy { (1...3).contains($0) })
        precondition(difficultyLevels == Array(Set(difficultyLevels)).sorted())
        self.difficultyLevels = difficultyLevels
        self.providesTimeEstimates = providesTimeEstimates
    }
}

/// A metadata-only resource record; editorial text and third-party assets are never bundled here.
public struct LibraryResource: Codable, Hashable, Identifiable {
    public let id: String
    public let title: String
    public let category: ResourceCategory
    public let source: ResourceSourceMetadata
    public let linkScope: ResourceLinkScope
    /// Search labels only; not summaries or copied source text.
    public let searchTerms: [String]
    public let guideMetadata: ResourceGuideMetadata?

    public var canonicalURL: String { source.canonicalURL }

    public init(
        id: String,
        title: String,
        category: ResourceCategory,
        source: ResourceSourceMetadata,
        linkScope: ResourceLinkScope = .directPage,
        searchTerms: [String] = [],
        guideMetadata: ResourceGuideMetadata? = nil
    ) {
        self.id = id
        self.title = title
        self.category = category
        self.source = source
        self.linkScope = linkScope
        self.searchTerms = searchTerms
        self.guideMetadata = guideMetadata
    }
}

/// Pure, testable index and query helpers for native clients; UI and route handling remain elsewhere.
public enum ResourceLibrary {
    /// Date the supplied official-site research was reviewed: 2026-10-29 UTC.
    public static let reviewedAtEpochMillis: Int64 = 1_793_232_000_000
    private static let reviewIntervalDays = 180

    public static let resources: [LibraryResource] = [
        resource(
            id: "resource-library", title: "Resource Library", category: .reference,
            canonicalPath: "eg-resources",
            searchTerms: ["resources", "lessons", "videos", "toolkits", "guides", "pdf", "drive", "docs"]
        ),
        resource(
            id: "crews", title: "Crews", category: .community, canonicalPath: "crews",
            searchTerms: ["signup", "onboarding", "mentorship", "regional crews counselor", "interactive maps"]
        ),
        resource(
            id: "project-grants", title: "Project Grants", category: .funding, canonicalPath: "project-grants",
            freshnessHint: .reviewRecommended,
            searchTerms: ["microgrants", "application", "crew funding", "closed application"]
        ),
        resource(
            id: "project-guides-toolkits", title: "Project Guides & Toolkits", category: .projectGuides,
            canonicalPath: "eg-projectguides-toolkits",
            guideMetadata: ResourceGuideMetadata(difficultyLevels: [1, 2, 3], providesTimeEstimates: true),
            searchTerms: [
                "choose action now", "climate-friendly banking", "farmers markets", "lobbying",
                "legislation", "community fridges", "free markets", "food forests",
                "guerilla gardening", "native planting", "cleanups", "art activism",
                "nonviolent direct action"
            ]
        ),
        resource(
            id: "events-calendar", title: "Events", category: .calendar, canonicalPath: "events",
            freshnessHint: .reviewRecommended,
            searchTerms: ["calendar", "upcoming", "event", "ics", "google calendar"]
        ),
        resource(
            id: "youth-video-lessons", title: "Youth Lessons & Videos", category: .learning,
            canonicalPath: "eg-resources", sourcePath: "eg-resources", linkScope: .collectionFallback,
            searchTerms: ["youtube", "video", "lesson", "lesson plans", "youth education"]
        ),
        resource(
            id: "youth-written-blog", title: "Youth-Written Blog", category: .learning,
            canonicalPath: "earthguardian-blog", sourcePath: "eg-resources",
            searchTerms: ["stories", "blog", "youth writing"]
        ),
        resource(
            id: "classroom-and-home-tools", title: "Classroom, Crew & Home Tools", category: .learning,
            canonicalPath: "eg-resources", sourcePath: "eg-resources", linkScope: .collectionFallback,
            searchTerms: [
                "classroom", "school crew", "crew guides", "permaculture", "divestment",
                "50 simple things", "home environmental audit", "school environmental audit"
            ]
        ),
        resource(
            id: "campaigns", title: "Campaigns", category: .campaigns, canonicalPath: "campaigns-2",
            searchTerms: ["choose action now", "stop the money pipeline", "civic engagement", "divestment"]
        ),
        resource(
            id: "choose-action-now", title: "Choose Action Now", category: .campaigns,
            canonicalPath: "can", sourcePath: "campaigns-2",
            searchTerms: ["can", "youth global day of action"]
        ),
        resource(
            id: "stop-the-money-pipeline", title: "Stop the Money Pipeline", category: .campaigns,
            canonicalPath: "stmp", sourcePath: "campaigns-2",
            searchTerms: ["stmp", "divestment", "finance"]
        ),
        resource(
            id: "civic-engagement", title: "Civic Engagement", category: .campaigns,
            canonicalPath: "civic-engagement", sourcePath: "campaigns-2",
            searchTerms: ["voting", "legislation", "policy"]
        ),
        resource(
            id: "divest-and-defund", title: "Divest & Defund", category: .campaigns,
            canonicalPath: "divest-defund", sourcePath: "campaigns-2",
            searchTerms: ["divestment", "fossil fuels", "finance"]
        ),
        resource(
            id: "ej-principles", title: "Environmental Justice Principles", category: .reference,
            canonicalPath: "principles-of-environmental-justice", sourcePath: "eg-resources",
            searchTerms: ["environmental justice", "ej principles"]
        ),
        resource(
            id: "sustainable-development-goals", title: "Sustainable Development Goals", category: .reference,
            canonicalPath: "sdgs", sourcePath: "eg-resources",
            searchTerms: ["sdgs", "un sustainable development goals"]
        ),
        resource(
            id: "glossary", title: "Dictionary of Important Terms", category: .reference,
            canonicalPath: "dictionary-of-important-terms", sourcePath: "eg-resources",
            searchTerms: ["glossary", "intersectionality", "allyship", "place-based education"]
        ),
        resource(
            id: "speakers-bureau", title: "Speakers’ Bureau", category: .community,
            canonicalPath: "speakers-bureau", sourcePath: "eg-resources",
            searchTerms: ["speakers", "youth speakers", "booking"]
        ),
        resource(
            id: "programs-overview", title: "Programs", category: .programs,
            canonicalPath: "eg-programs", sourcePath: "eg-programs",
            searchTerms: ["crews", "rcc", "iyi", "speakers bureau"]
        ),
        resource(
            id: "indigenous-youth-initiative", title: "Indigenous Youth Initiative", category: .programs,
            canonicalPath: "indigenous-youth-initiative", sourcePath: "eg-programs",
            freshnessHint: .timeSensitive, opportunityEndsAtEpochMillis: 1_789_689_600_000,
            searchTerms: ["iyi", "training", "october 2026"]
        )
    ]

    public static func byID(_ id: String) -> LibraryResource? {
        resources.first { $0.id == id }
    }

    /// Search by title, stable ID or short labels, with optional category/difficulty filters.
    public static func search(
        query: String = "",
        category: ResourceCategory? = nil,
        difficultyLevel: Int? = nil
    ) -> [LibraryResource] {
        if let difficultyLevel, !(1...3).contains(difficultyLevel) { return [] }
        let normalizedQuery = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return resources.filter { item in
            let matchesCategory = category == nil || item.category == category
            let matchesDifficulty = difficultyLevel == nil ||
                item.guideMetadata?.difficultyLevels.contains(difficultyLevel!) == true
            let matchesQuery = normalizedQuery.isEmpty ||
                item.title.localizedCaseInsensitiveContains(normalizedQuery) ||
                item.id.localizedCaseInsensitiveContains(normalizedQuery) ||
                item.searchTerms.contains { $0.localizedCaseInsensitiveContains(normalizedQuery) }
            return matchesCategory && matchesDifficulty && matchesQuery
        }
    }

    private static func resource(
        id: String,
        title: String,
        category: ResourceCategory,
        canonicalPath: String,
        sourcePath: String? = nil,
        linkScope: ResourceLinkScope = .directPage,
        freshnessHint: ResourceFreshnessHint = .reviewed,
        opportunityEndsAtEpochMillis: Int64? = nil,
        guideMetadata: ResourceGuideMetadata? = nil,
        searchTerms: [String] = []
    ) -> LibraryResource {
        let siteRoot = SitePaths.siteURL
        let source = ResourceSourceMetadata(
            canonicalURL: "\(siteRoot)/\(canonicalPath)",
            sourceURL: "\(siteRoot)/\(sourcePath ?? canonicalPath)",
            reviewedAtEpochMillis: reviewedAtEpochMillis,
            reviewIntervalDays: reviewIntervalDays,
            freshnessHint: freshnessHint,
            opportunityEndsAtEpochMillis: opportunityEndsAtEpochMillis
        )
        return LibraryResource(
            id: id,
            title: title,
            category: category,
            source: source,
            linkScope: linkScope,
            searchTerms: searchTerms,
            guideMetadata: guideMetadata
        )
    }
}
