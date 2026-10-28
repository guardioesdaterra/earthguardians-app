package org.earthguardians.app.data.parse

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Parses the Squarespace feed (`?format=RSS`) used by the EG blog and events pages.
 * Namespace-agnostic: content:encoded and media:* are matched by local name.
 */
object RssParser {

    data class RssItem(
        val title: String? = null,
        val link: String? = null,
        val description: String? = null,
        val contentHtml: String? = null,
        val pubDate: String? = null,
        /** First image from media:thumbnail, media:content or enclosure, if any. */
        val imageUrl: String? = null,
        val category: String? = null,
    )

    fun parse(rawXml: String): List<RssItem> = parse(StringReader(rawXml))

    fun parse(reader: Reader): List<RssItem> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(reader)

        val items = mutableListOf<RssItem>()
        var current: MutableRssItem? = null
        var text = StringBuilder()
        // A truncated feed must surface as an exception instead of silently
        // returning fewer items than the feed promised.
        var depth = 0
        // Only these element types own buffered text; nested markup inside the
        // (CDATA-wrapped) description/content must not wipe the buffer mid-tag.
        val handled = setOf("item", "title", "link", "description", "encoded", "pubDate", "category")

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name.orEmpty().substringAfterLast(':')
            when (event) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (name in handled) text = StringBuilder()
                    when {
                        name == "item" -> current = MutableRssItem()
                        name == "enclosure" && current != null -> {
                            val url = parser.getAttributeValue(null, "url")
                            if (url != null && current.imageUrl == null) current.imageUrl = url
                        }
                        (name == "thumbnail" || name == "content") && current != null -> {
                            val url = parser.getAttributeValue(null, "url")
                            if (url != null && current.imageUrl == null) current.imageUrl = url
                        }
                    }
                }
                XmlPullParser.TEXT -> text.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> {
                    depth = (depth - 1).coerceAtLeast(0)
                    val item = current
                    if (item != null) {
                        when (name) {
                            "item" -> {
                                items += item.toRssItem()
                                current = null
                            }
                            "title" -> if (item.title == null) item.title = text.trimOrEmpty()
                            "link" -> if (item.link == null && text.isNotBlank()) item.link = text.trimOrEmpty()
                            "description" -> if (item.description == null) item.description = EgText.decodeFeedEntities(text.trimOrEmpty())
                            "content:encoded", "encoded" -> if (item.contentHtml == null) item.contentHtml = text.orNullIfBlank()
                            "pubDate" -> if (item.pubDate == null) item.pubDate = text.trimOrEmpty()
                            "category" -> if (item.category == null && text.isNotBlank()) item.category = text.trimOrEmpty()
                        }
                    }
                    if (name in handled) text = StringBuilder()
                }
            }
            event = parser.next()
        }
        // XmlPullParser stops at end-of-stream even with open elements — a
        // non-zero depth here means the feed XML is malformed/truncated.
        if (depth > 0) throw XmlPullParserException("Unexpected end of document — feed XML is truncated", parser, null)
        return items
    }

    private fun StringBuilder.trimOrEmpty(): String = toString().trim { it.isWhitespace() }
    private fun StringBuilder.orNullIfBlank(): String? =
        toString().trim().ifEmpty { null }

    private fun StringBuilder.isNotBlank(): Boolean = this.trim().isNotEmpty()
    private fun StringBuilder.trim(): String = toString().trim()

    private class MutableRssItem {
        var title: String? = null
        var link: String? = null
        var description: String? = null
        var contentHtml: String? = null
        var pubDate: String? = null
        var imageUrl: String? = null
        var category: String? = null

        fun toRssItem() = RssItem(
            title = title,
            link = link,
            description = description,
            contentHtml = contentHtml,
            pubDate = pubDate,
            imageUrl = imageUrl,
            category = category,
        )
    }

    private val rssDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** Squarespace pubDate → epoch millis (null when absent/unparseable). */
    fun parsePubDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching { rssDateFormat.parse(raw)?.time }.getOrNull()
    }

    fun formatEpochShort(epoch: Long?): String {
        if (epoch == null || epoch <= 0L) return "—"
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epoch))
    }
}

/**
 * Lightweight sitemap parser — collects `<url><loc>` entries from the standard
 * sitemap.org urlset (which is what earthguardians.org publishes).
 */
object SitemapParser {

    fun parse(rawXml: String): List<String> = parse(StringReader(rawXml))

    fun parse(reader: Reader): List<String> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(reader)

        val urls = mutableListOf<String>()
        var insideLoc = false
        var text = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name.orEmpty().substringAfterLast(':')
            when (event) {
                XmlPullParser.START_TAG -> {
                    insideLoc = name == "loc"
                    text = StringBuilder()
                }
                XmlPullParser.TEXT -> if (insideLoc) text.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> {
                    if (name == "loc") {
                        val loc = text.toString().trim()
                        if (loc.startsWith("http")) urls += loc
                    }
                    insideLoc = false
                }
            }
            event = parser.next()
        }
        return urls
    }
}
