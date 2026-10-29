import XCTest
@testable import EGCore

final class ResourceLibraryTests: XCTestCase {
    func testCatalogIDsAreUniqueAndEveryRecordHasOfficialSourceMetadata() {
        let ids = ResourceLibrary.resources.map(\.id)
        XCTAssertEqual(ids.count, Set(ids).count)
        for item in ResourceLibrary.resources {
            XCTAssertFalse(item.id.isEmpty)
            XCTAssertFalse(item.title.isEmpty)
            XCTAssertTrue(item.source.canonicalURL.hasPrefix("https://www.earthguardians.org/"))
            XCTAssertTrue(item.source.sourceURL.hasPrefix("https://www.earthguardians.org/"))
            XCTAssertGreaterThan(item.source.reviewedAtEpochMillis, 0)
        }
    }

    func testUnresolvedThirdPartyLinksFallBackToAnOfficialCollection() {
        let fallbackItems = ResourceLibrary.resources.filter { $0.linkScope == .collectionFallback }
        XCTAssertFalse(fallbackItems.isEmpty)
        for item in fallbackItems {
            XCTAssertEqual(item.canonicalURL, "\(SitePaths.siteURL)/eg-resources")
        }
    }

    func testGuideLevelsAndTimeEstimatesAreCollectionFacts() throws {
        let guides = try XCTUnwrap(ResourceLibrary.byID("project-guides-toolkits"))
        XCTAssertEqual(guides.guideMetadata?.difficultyLevels, [1, 2, 3])
        XCTAssertEqual(guides.guideMetadata?.providesTimeEstimates, true)
        XCTAssertTrue(ResourceLibrary.resources
            .filter { $0.id != "project-guides-toolkits" }
            .allSatisfy { $0.guideMetadata == nil })
    }

    func testDatedGrantsAndEventsAreFlaggedForReview() throws {
        for id in ["project-grants", "events-calendar"] {
            let item = try XCTUnwrap(ResourceLibrary.byID(id))
            XCTAssertEqual(item.source.freshnessHint, .reviewRecommended)
            XCTAssertEqual(
                item.source.freshness(atEpochMillis: ResourceLibrary.reviewedAtEpochMillis),
                .reviewRecommended
            )
        }
    }

    func testExpiredTrainingOpportunityDoesNotMeanThePageExpired() throws {
        let item = try XCTUnwrap(ResourceLibrary.byID("indigenous-youth-initiative"))
        XCTAssertEqual(item.source.freshnessHint, .timeSensitive)
        XCTAssertEqual(item.source.freshness(atEpochMillis: 1_789_689_599_999), .timeSensitive)
        XCTAssertEqual(item.source.freshness(atEpochMillis: 1_789_689_600_000), .opportunityExpired)
        XCTAssertTrue(item.canonicalURL.hasSuffix("/indigenous-youth-initiative"))
    }

    func testSearchSupportsLabelsAndNativeFilters() {
        XCTAssertEqual(
            ResourceLibrary.search(query: "  FARMERS  ", category: .projectGuides).map(\.id),
            ["project-guides-toolkits"]
        )
        XCTAssertEqual(ResourceLibrary.search(difficultyLevel: 2).map(\.id), ["project-guides-toolkits"])
        XCTAssertTrue(ResourceLibrary.search(difficultyLevel: 4).isEmpty)
        XCTAssertEqual(ResourceLibrary.byID("crews")?.id, "crews")
        XCTAssertNil(ResourceLibrary.byID("unknown"))
    }

    func testCatalogStoresNoEditorialSummariesOrMediaPayloads() {
        XCTAssertFalse(ResourceLibrary.resources.contains { $0.searchTerms.contains(where: \.isEmpty) })
        XCTAssertFalse(ResourceLibrary.resources.contains { $0.source.canonicalURL.contains("drive.google.com") })
    }
}
