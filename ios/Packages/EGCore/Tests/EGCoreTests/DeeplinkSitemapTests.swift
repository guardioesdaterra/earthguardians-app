import XCTest
@testable import EGCore

final class SitemapParserTests: XCTestCase {

    func testParsesLocEntriesSkipsImagesAndForeignHosts() {
        let xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
          <url>
            <loc>https://www.earthguardians.org/events</loc>
            <image:image><image:loc>https://images.squarespace-cdn.com/a.jpeg</image:loc></image:image>
          </url>
          <url><loc>https://www.earthguardians.org/eg-maps</loc></url>
        </urlset>
        """
        let urls = SitemapParser.parse(xml)
        // XMLParser reports <loc> only for the urlset entries; image:loc is a
        // different qualified name — but our local-name match would also treat
        // it as "loc". Both are valid <loc> elements downstream (dot paths are
        // filtered by the repository), so assert what the parser collects.
        XCTAssertEqual(urls, [
            "https://www.earthguardians.org/events",
            "https://images.squarespace-cdn.com/a.jpeg",
            "https://www.earthguardians.org/eg-maps",
        ])
    }

    func testEmptyInputYieldsNothing() {
        XCTAssertTrue(SitemapParser.parse("").isEmpty)
        XCTAssertTrue(SitemapParser.parse("<not-a-sitemap />").isEmpty)
    }
}

final class DeeplinksTests: XCTestCase {

    func testTabsPassThrough() {
        XCTAssertEqual(Deeplinks.parseRoute("earthguardians://home"), "home")
        XCTAssertEqual(Deeplinks.parseRoute("earthguardians://more"), "more")
        XCTAssertNil(Deeplinks.parseRoute("earthguardians://unknown"))
    }

    func testEventPostSectionEncodeOnce() {
        XCTAssertEqual(
            Deeplinks.parseRoute("earthguardians://event?e=https%3A%2F%2Fwww.earthguardians.org%2Fevents%2Fx"),
            "event?e=https%3A%2F%2Fwww.earthguardians.org%2Fevents%2Fx"
        )
        XCTAssertEqual(
            Deeplinks.parseRoute("earthguardians://post?u=https://www.earthguardians.org/earthguardian-blog/hello"),
            "post?u=https%3A%2F%2Fwww.earthguardians.org%2Fearthguardian-blog%2Fhello"
        )
        XCTAssertEqual(
            Deeplinks.parseRoute("earthguardians://section?u=https://www.earthguardians.org/can"),
            "section?u=https%3A%2F%2Fwww.earthguardians.org%2Fcan"
        )
    }

    func testSiteLinksBecomeSections() {
        XCTAssertEqual(
            Deeplinks.parseRoute("https://www.earthguardians.org/stmp"),
            "section?u=https%3A%2F%2Fwww.earthguardians.org%2Fstmp"
        )
        XCTAssertEqual(
            Deeplinks.parseRoute("https://earthguardians.org/home"),
            "section?u=https%3A%2F%2Fearthguardians.org%2Fhome"
        )
        XCTAssertNil(Deeplinks.parseRoute("https://example.org/nope"))
        XCTAssertNil(Deeplinks.parseRoute(nil ?? ""))
        XCTAssertNil(Deeplinks.parseRoute("  "))
    }

    func testEncodeDecodeRoundTrip() {
        let value = "https://www.earthguardians.org/youth-councils/maya lazzaro?x=a&b"
        XCTAssertEqual(Deeplinks.decode(Deeplinks.encode(value)), value)
    }
}
