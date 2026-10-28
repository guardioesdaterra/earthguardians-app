package org.earthguardians.app.data.parse

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the sitemap.org urlset parser. */
class SitemapParserTest {

    private val sitemapXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
          <url>
            <loc>https://www.earthguardians.org/</loc>
            <lastmod>2024-03-01</lastmod>
          </url>
          <url>
            <loc>https://www.earthguardians.org/eg-maps</loc>
          </url>
          <url>
            <loc>https://www.earthguardians.org/earthguardian-blog/trees-not-racism</loc>
          </url>
          <url>
            <loc></loc>
          </url>
          <url>
            <loc>ftp://not-a-web-url.example</loc>
          </url>
        </urlset>
    """.trimIndent()

    @Test
    fun `extracts all loc entries in order`() {
        val urls = SitemapParser.parse(StringReader(sitemapXml))
        assertEquals(3, urls.size)
        assertEquals(
            listOf(
                "https://www.earthguardians.org/",
                "https://www.earthguardians.org/eg-maps",
                "https://www.earthguardians.org/earthguardian-blog/trees-not-racism",
            ),
            urls,
        )
    }

    @Test
    fun `trims whitespace around loc text`() {
        val urls = SitemapParser.parse(StringReader(sitemapXml))
        urls.forEach { assertEquals(it, it.trim()) }
    }

    @Test
    fun `real earthguardians sitemap shape parses`() {
        // Mirrors the actual namespace-declared structure of the live sitemap,
        // including a sitemap index child element that must be ignored.
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
              <url><loc>https://www.earthguardians.org/take-action</loc></url>
              <url><loc>https://www.earthguardians.org/speakers-bureau</loc></url>
              <sitemapindex></sitemapindex>
            </urlset>
        """.trimIndent()
        val urls = SitemapParser.parse(StringReader(xml))
        assertEquals(2, urls.size)
        assertTrue(urls.first().endsWith("/take-action"))
    }

    @Test
    fun `empty input yields empty list`() {
        assertEquals(0, SitemapParser.parse(StringReader("")).size)
        assertEquals(0, SitemapParser.parse(StringReader("<urlset></urlset>")).size)
    }
}
