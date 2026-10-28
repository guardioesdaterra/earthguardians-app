import XCTest
@testable import EGCore

final class SiteCatalogTests: XCTestCase {

    func testCatalogPathsAreUnique() {
        let paths = SiteCatalog.sections.map(\.path)
        XCTAssertEqual(paths.count, Set(paths).count, "duplicate paths in catalog")
    }

    func testEverySectionResolvesEveryGroup() {
        for section in SiteCatalog.sections {
            XCTAssertTrue(section.url.hasPrefix(SitePaths.siteURL), "bad url \(section.url)")
            XCTAssertFalse(section.title.isEmpty)
        }
        XCTAssertTrue(SectionGroup.allCases.allSatisfy { group in
            SiteCatalog.sections.contains { $0.group == group }
        }, "each group must have entries")
    }

    func testMapsPathResolves() {
        XCTAssertEqual(SiteCatalog.byPath("eg-maps")?.title, "EG-Maps")
        XCTAssertEqual(SiteCatalog.byPath("endangered-species-map")?.group, .explore)
        XCTAssertNil(SiteCatalog.byPath("does-not-exist"))
    }

    func testImpactStatsMatchThe2024ImpactReport() {
        XCTAssertEqual(SiteCatalog.Impact.directlyImpacted, "1M+")
        XCTAssertEqual(SiteCatalog.Impact.trees, "20K+")
        XCTAssertEqual(SiteCatalog.Impact.youthLed, "100%")
        XCTAssertEqual(SiteCatalog.Impact.youthStaff, "94%")
    }

    func testDeadSitePagesAreNeverLinked() {
        // /youth-councils and /regional-directors 404 on the live site (checked
        // Oct 2026); the real pages are the profile sub-pages and eg-team.
        XCTAssertNil(SiteCatalog.byPath("youth-councils"))
        XCTAssertNil(SiteCatalog.byPath("regional-directors"))
    }

    func testOurPeopleGroupCarriesLeadershipAndSpeakerProfiles() {
        let people = SiteCatalog.sections.filter { $0.group == .people }
        XCTAssertGreaterThanOrEqual(people.count, 10, "expected the full people roster")
        XCTAssertTrue(people.contains { $0.path == "tyler-massias" })
        XCTAssertTrue(people.contains { $0.path == "catherine-mongella-speakersbureau" })
        XCTAssertTrue(people.contains { $0.path == "youth-councils/nina-berglund" })
        // Sub-collection profile pages must keep their folder prefix.
        XCTAssertTrue(people.contains { $0.url.contains("/youth-councils/") })
    }
}

final class ContentRepositoryMappingTests: XCTestCase {

    func testMapsPostsOnlyFromBlogPaths() throws {
        let items = RssParser.parse("""
        <rss><channel>
        // Note: deliberately no <title> — exercises the slug → title fallback.
        <item><link>https://www.earthguardians.org/earthguardian-blog/hello-world</link><description>&lt;b&gt;Teaser&lt;/b&gt;</description><pubDate>Fri, 01 Mar 2024 10:00:00 GMT</pubDate></item>
        <item><title>Not blog</title><link>https://www.earthguardians.org/random-page</link></item>
        </channel></rss>
        """)
        let posts = ContentRepository.mapPosts(items)
        XCTAssertEqual(posts.count, 1)
        XCTAssertEqual(posts[0].title, "Hello World")
        XCTAssertEqual(posts[0].teaser, "Teaser")
        XCTAssertEqual(try XCTUnwrap(posts[0].publishedAt).timeIntervalSince1970, 1709287200.0, accuracy: 1)
    }

    func testMapsEventsWithSlugDateFallback() {
        let items = RssParser.parse("""
        <rss><channel>
        <item><link>https://www.earthguardians.org/events/2025/6/14/project-grant-summit</link></item>
        <item><link>https://www.earthguardians.org/other/place</link></item>
        </channel></rss>
        """)
        let events = ContentRepository.mapEvents(items)
        XCTAssertEqual(events.count, 1)
        XCTAssertEqual(events[0].title, "Project Grant Summit")
        let date = events[0].startsAt
        XCTAssertNotNil(date)
        var comps = DateComponents()
        (comps.year, comps.month, comps.day) = (2025, 6, 14)
        let expected = Calendar.gregorianUTCFallback.date(from: comps)
        XCTAssertEqual(date?.timeIntervalSince1970, expected?.timeIntervalSince1970)
    }
}
