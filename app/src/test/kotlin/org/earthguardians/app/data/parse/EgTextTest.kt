package org.earthguardians.app.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure-Kotlin HTML/text helpers. */
class EgTextTest {

    @Test
    fun `fromHtml strips tags and collapses whitespace`() {
        assertEquals(
            "One ripple at a time.",
            EgText.fromHtml("<p>One    ripple</p>\n  <br><span>at a time.</span>"),
        )
    }

    @Test
    fun `fromHtml decodes feed entities`() {
        // One apostrophe entity is enough — the expected string matches
        // &amp; &lt; &gt; &quot; &#39; &nbsp; decoded.
        assertEquals("& < > \" ' —", EgText.fromHtml("&amp; &lt; &gt; &quot; &#39; &nbsp;—"))
    }

    @Test
    fun `fromHtml handles null, blank and tag-only html`() {
        assertEquals("", EgText.fromHtml(null))
        assertEquals("", EgText.fromHtml("   "))
        assertEquals("", EgText.fromHtml("<b></b> <br/>"))
    }

    @Test
    fun `titleFromSlug title-cases hyphenated slugs`() {
        assertEquals("Trees Not Racism", EgText.titleFromSlug("trees-not-racism"))
        assertEquals("Of The Land Revisioning Climate", EgText.titleFromSlug("of-the-land-revisioning-climate"))
    }

    @Test
    fun `titleFromSlug ignores query, fragment and repeated separators`() {
        assertEquals("Shop Meet", EgText.titleFromSlug("shop--meet?#ref=1"))
    }

    @Test
    fun `titleFromSlug handles blank and single words`() {
        assertEquals("", EgText.titleFromSlug(""))
        assertEquals("", EgText.titleFromSlug("   "))
        assertEquals("Water", EgText.titleFromSlug("water"))
    }

    @Test
    fun `slugOf returns last meaningful path segment`() {
        assertEquals(
            "first-story",
            EgText.slugOf("https://www.earthguardians.org/earthguardian-blog/first-story"),
        )
        assertEquals("eg-maps", EgText.slugOf("https://www.earthguardians.org/eg-maps/"))
        assertEquals("home", EgText.slugOf("https://www.earthguardians.org/home?utm=ig"))
    }

    @Test
    fun `round trip slugOf into titleFromSlug`() {
        val url = "https://www.earthguardians.org/earthguardian-blog/trees-not-racism/"
        val title = EgText.titleFromSlug(EgText.slugOf(url))
        assertEquals("Trees Not Racism", title)
        assertTrue(title.isNotBlank())
    }
}
