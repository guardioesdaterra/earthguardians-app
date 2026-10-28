import XCTest
@testable import EGCore

final class RssParserTests: XCTestCase {

    func testParsesBasicItemsWithCDATAContent() throws {
        let xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:content="http://purl.org/rss/1.0/modules/content/">
        <channel>
          <item>
            <title><![CDATA[ Frontlines Dispatch: Water is Life ]]></title>
            <link>https://www.earthguardians.org/earthguardian-blog/2024/3/2/frontlines-dispatch</link>
            <description><![CDATA[ <p>Youth on the <b>frontlines</b></p> ]]></description>
            <content:encoded><![CDATA[ <div>Full story body</div> ]]></content:encoded>
            <pubDate>Sat, 02 Mar 2024 18:45:00 GMT</pubDate>
            <media:content url="https://images.squarespace-cdn.com/hero.jpeg"/>
            <category>climate</category>
          </item>
        </channel>
        </rss>
        """
        let items = RssParser.parse(xml)
        XCTAssertEqual(items.count, 1)
        let item = items[0]
        XCTAssertEqual(item.title, "Frontlines Dispatch: Water is Life")
        XCTAssertEqual(item.link, "https://www.earthguardians.org/earthguardian-blog/2024/3/2/frontlines-dispatch")
        XCTAssertEqual(item.contentHtml, "<div>Full story body</div>")
        XCTAssertEqual(item.imageUrl, "https://images.squarespace-cdn.com/hero.jpeg")
        XCTAssertEqual(item.category, "climate")

        let date = RssParser.parsePubDate(item.pubDate)
        XCTAssertNotNil(date)
        XCTAssertEqual(try XCTUnwrap(date).timeIntervalSince1970, 1709405100.0, accuracy: 1)
    }

    func testEnclosureImageFallbackAndNoCDATAText() {
        let xml = """
        <rss><channel><item>
          <title>Plain title</title>
          <link>https://www.earthguardians.org/events/2025/6/14/project-grant-summit</link>
          <description>Plain teaser with HTML in <b>?</b></description>
          <pubDate>Tue, 10 Dec 2024 09:00:00 +0000</pubDate>
          <enclosure url="https://images.squarespace-cdn.com/enclosure.jpeg"/>
        </item></channel></rss>
        """
        let items = RssParser.parse(xml)
        XCTAssertEqual(items.count, 1)
        XCTAssertEqual(items[0].imageUrl, "https://images.squarespace-cdn.com/enclosure.jpeg")
        XCTAssertEqual(items[0].description, "Plain teaser with HTML in ?")
        XCTAssertNil(items[0].contentHtml)
    }

    func testMultipleItemsOrderStable() {
        let xml = """
        <rss><channel>
        <item><title>A</title><link>https://x/a</link></item>
        <item><title>B</title><link>https://x/b</link></item>
        </channel></rss>
        """
        let items = RssParser.parse(xml)
        XCTAssertEqual(items.map(\.title), ["A", "B"])
    }
}

final class EgTextTests: XCTestCase {

    func testFromHtmlStripsTagsAndEntities() {
        XCTAssertEqual(EgText.fromHtml("<p>Hello&nbsp;<b>world</b>!</p>"), "Hello world!")
        XCTAssertEqual(EgText.fromHtml("A &amp; B"), "A & B")
        XCTAssertEqual(EgText.fromHtml(nil), "")
        XCTAssertEqual(EgText.fromHtml("   "), "")
    }

    func testTitleFromSlug() {
        XCTAssertEqual(EgText.titleFromSlug("of-the-land-revisioning-climate"), "Of The Land Revisioning Climate")
        XCTAssertEqual(EgText.titleFromSlug("single"), "Single")
        XCTAssertEqual(EgText.titleFromSlug(""), "")
    }

    func testSlugOfTail() {
        XCTAssertEqual(EgText.slugOf("https://www.earthguardians.org/our-story?x=1"), "our-story")
        XCTAssertEqual(EgText.slugOf("https://site.org/events/2025/6/14/summit/"), "summit")
    }
}
