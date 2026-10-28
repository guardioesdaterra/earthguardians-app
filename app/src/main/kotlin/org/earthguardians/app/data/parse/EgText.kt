package org.earthguardians.app.data.parse

/** Pure-Kotlin HTML/text helpers (unit-testable, no Android deps). */
object EgText {

    // Letter-anchored so decoded plain-text `<`/`>` can't form a "tag": the
    // literal "< >" produced by "&lt; &gt;" must survive fromHtml.
    private val tagRegex = Regex("</?[A-Za-z][^>]*>")
    private val whitespaceRegex = Regex("\\s+")

    /** Entities HTML feeds use. `&apos;` is XML syntax, not an HTML entity, so it
     *  is dropped — Squarespace feed text writes apostrophes as `&#39;`. */
    private val entities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&apos;" to "",
        "&nbsp;" to " ",
    )

    /**
     * Decodes entities, strips tags and collapses whitespace for display text.
     * Decode runs FIRST so entities inside already-stripped markup don't leak
     * (`&lt;b&gt;Teaser&lt;/b&gt;` and `<b>Teaser</b>` both read as "Teaser").
     */
    fun fromHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var out = raw.orEmpty()
        entities.forEach { (entity, replacement) -> out = out.replace(entity, replacement) }
        out = tagRegex.replace(out, "")
        return whitespaceRegex.replace(out, " ").trim()
    }

    /** Entity-only decode (tags preserved) for raw CDATA text collected by the parser. */
    fun decodeFeedEntities(raw: String?): String {
        if (raw == null) return ""
        var out = raw
        entities.forEach { (entity, replacement) -> out = out.replace(entity, replacement) }
        return out
    }

    /** "of-the-land-revisioning-climate" → "Of The Land Revisioning Climate". */
    fun titleFromSlug(slug: String): String {
        if (slug.isBlank()) return ""
        return slug
            .substringBefore('?')
            .substringBefore('#')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    /** Last meaningful path segment of a URL, used for fallback titles. */
    fun slugOf(url: String): String {
        val trimmed = url.substringBefore('?').trimEnd('/').substringAfter('/')
        val tail = trimmed.substringAfterLast('/')
        return tail
    }
}
