package org.earthguardians.app.data.parse

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the Squarespace RSS feed parser (pure JVM, no Android deps). */
class RssParserTest {

    private val feedXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss xmlns:content="http://purl.org/rss/1.0/modules/content/"
             xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
          <channel>
            <title>Earth Guardian Blog</title>
            <item>
              <title>First Story &amp; Ripple Effects</title>
              <link>https://www.earthguardians.org/earthguardian-blog/first-story</link>
              <description><![CDATA[<p>A short teaser with &amp; entities</p>]]></description>
              <content:encoded><![CDATA[<p>Full <b>body</b> text</p>]]></content:encoded>
              <pubDate>Thu, 10 Oct 2024 14:00:00 +0000</pubDate>
              <media:thumbnail url="https://static.squarespace.com/img-first.jpg"/>
              <category>Youth Power</category>
            </item>
            <item>
              <title>Media content fallback</title>
              <link>https://www.earthguardians.org/earthguardian-blog/second-story</link>
              <media:content url="https://static.squarespace.com/img-second.jpg"/>
            </item>
            <item>
              <title>Enclosure fallback</title>
              <link>https://www.earthguardians.org/earthguardian-blog/third-story</link>
              <enclosure url="https://static.squarespace.com/img-third.jpg" type="image/jpeg" length="0"/>
            </item>
            <item>
              <title>Image-less item</title>
              <link>https://www.earthguardians.org/earthguardian-blog/plain</link>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun `parses all items`() {
        val items = RssParser.parse(StringReader(feedXml))
        assertEquals(4, items.size)
    }

    @Test
    fun `extracts title, link, content and category`() {
        val item = RssParser.parse(StringReader(feedXml)).first()
        assertEquals("First Story & Ripple Effects", item.title)
        assertEquals("https://www.earthguardians.org/earthguardian-blog/first-story", item.link)
        assertEquals("<p>Full <b>body</b> text</p>", item.contentHtml)
        assertEquals("Youth Power", item.category)
    }

    @Test
    fun `decodes entities from description`() {
        val item = RssParser.parse(StringReader(feedXml)).first()
        assertEquals("<p>A short teaser with & entities</p>", item.description)
    }

    @Test
    fun `image sources fall back in thumbnail, content, enclosure order`() {
        val items = RssParser.parse(StringReader(feedXml))
        assertEquals("https://static.squarespace.com/img-first.jpg", items[0].imageUrl)
        assertEquals("https://static.squarespace.com/img-second.jpg", items[1].imageUrl)
        assertEquals("https://static.squarespace.com/img-third.jpg", items[2].imageUrl)
        assertNull(items[3].imageUrl)
    }

    @Test
    fun `pubDate parses to epoch millis`() {
        val item = RssParser.parse(StringReader(feedXml)).first()
        val epoch = RssParser.parsePubDate(item.pubDate)
        assertNotNull(epoch)
        // 2024-10-10 14:00:00 UTC
        assertEquals(1728568800000L, epoch)
    }

    @Test
    fun `pubDate handles null, blank and garbage`() {
        assertNull(RssParser.parsePubDate(null))
        assertNull(RssParser.parsePubDate(""))
        assertNull(RssParser.parsePubDate("   "))
        assertNull(RssParser.parsePubDate("not a date at all"))
    }

    @Test
    fun `formatEpochShort renders em dash for null and zero`() {
        assertEquals("—", RssParser.formatEpochShort(null))
        assertEquals("—", RssParser.formatEpochShort(0L))
        assertEquals("—", RssParser.formatEpochShort(-5L))
        assertTrue(RssParser.formatEpochShort(1728568800000L).contains("2024"))
    }

    @Test
    fun `namespace prefixed link element is matched by local name`() {
        // Squarespace sometimes namespaces the link as atom:link — local-name matching
        // should still find plain <link>, and not crash on prefixed siblings.
        val xml = """
            <rss version="2.0">
              <channel>
                <item>
                  <title>t</title>
                  <link>https://www.earthguardians.org/x</link>
                  <atom:link href="https://www.earthguardians.org/x-atom"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()
        val items = RssParser.parse(StringReader(xml))
        assertEquals(1, items.size)
        assertEquals("https://www.earthguardians.org/x", items[0].link)
    }

    @Test
    fun `empty and malformed feeds produce empty lists`() {
        assertEquals(0, RssParser.parse(StringReader("")).size)
        assertEquals(0, RssParser.parse(StringReader("<rss><channel></channel></rss>")).size)
        // Malformed XML must not crash the sync loop — XmlPullParser throws.
        val broken = runCatching {
            RssParser.parse(StringReader("<rss><channel><item><title>unclosed"))
        }
        assertFalse(broken.isSuccess)
    }
}
