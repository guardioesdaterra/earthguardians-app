package org.earthguardians.app.data.parse

/** Pure-Kotlin HTML/text helpers (unit-testable, no Android deps). */
object EgText {

    private val tagRegex = Regex("<[^>]*>")
    private val whitespaceRegex = Regex("\\s+")

    private val entities = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&#39;" to "'",
        "&apos;" to "'",
        "&nbsp;" to " ",
    )

    /** Strips tags and decodes the handful of entities Squarespace feed text uses. */
    fun fromHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var out = raw
        entities.forEach { (entity, replacement) -> out = out.replace(entity, replacement) }
        out = tagRegex.replace(out, " ")
        return whitespaceRegex.replace(out, " ").trim()
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
