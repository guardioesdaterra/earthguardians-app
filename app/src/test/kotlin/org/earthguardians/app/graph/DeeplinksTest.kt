package org.earthguardians.app.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for pure-Kotlin deep-link routing. The percent-encoding contract
 * lives here: routes arriving from app shortcuts are pre-encoded, links pasted
 * from the browser are raw — both must normalize exactly once.
 */
class DeeplinksTest {

    private val donateUrl = "https://www.earthguardians.org/donate"
    private val postUrl = "https://www.earthguardians.org/earthguardian-blog/trees-not-racism"

    @Test
    fun `plain tab routes pass through`() {
        Deeplinks.NAV_TABS.forEach { tab ->
            assertEquals(tab, Deeplinks.parseRoute("earthguardians://$tab"))
            assertEquals(tab, Deeplinks.parseRoute("earthguardians://$tab/"))
        }
    }

    @Test
    fun `unknown tab scheme yields null`() {
        assertNull(Deeplinks.parseRoute("earthguardians://unknown"))
        assertNull(Deeplinks.parseRoute("earthguardians://"))
        assertNull(Deeplinks.parseRoute(null))
        assertNull(Deeplinks.parseRoute(""))
        assertNull(Deeplinks.parseRoute("   "))
    }

    @Test
    fun `pre-encoded shortcut urls are NOT double encoded`() {
        // App shortcuts are built as earthguardians://section?u=<encoded>
        val encoded = Deeplinks.encode(donateUrl) // https%3A%2F%2Fwww...
        val route = Deeplinks.parseRoute("earthguardians://section?u=$encoded")
        assertEquals("section?u=$encoded", route)
    }

    @Test
    fun `raw unencoded query values are encoded exactly once`() {
        val route = Deeplinks.parseRoute("earthguardians://post?u=$postUrl")
        assertEquals(
            "post?u=${Deeplinks.encode(postUrl)}",
            route,
        )
    }

    @Test
    fun `event routes keep their argument`() {
        val eventUrl = "https://www.earthguardians.org/events/climate-strike"
        val route = Deeplinks.parseRoute("earthguardians://event?e=${Deeplinks.encode(eventUrl)}")
        assertEquals("event?e=${Deeplinks.encode(eventUrl)}", route)
    }

    @Test
    fun `encode and decode round trip preserves urls`() {
        val route = Deeplinks.parseRoute("earthguardians://post?u=$postUrl")!!
        val restoredUrl = Deeplinks.decode(route.substringAfter("post?u="))
        assertEquals(postUrl, restoredUrl)
    }

    @Test
    fun `site pages route to the in-app section browser`() {
        assertEquals(
            "section?u=${Deeplinks.encode(donateUrl)}",
            Deeplinks.parseRoute(donateUrl),
        )
        assertEquals(
            "section?u=${Deeplinks.encode(donateUrl.replace("www.", ""))}",
            Deeplinks.parseRoute(donateUrl.replace("www.", "")),
        )
    }

    @Test
    fun `off-site urls do not route`() {
        assertNull(Deeplinks.parseRoute("https://example.com/donate"))
        assertNull(Deeplinks.parseRoute("ftp://www.earthguardians.org/x"))
        assertNull(Deeplinks.parseRoute("mailto:reachout@earthguardians.org"))
    }

    @Test
    fun `decode is null-safe`() {
        assertEquals("", Deeplinks.decode(null))
        assertEquals("a b", Deeplinks.decode("a%20b"))
    }
}
