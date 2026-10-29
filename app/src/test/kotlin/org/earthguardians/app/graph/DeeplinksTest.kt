package org.earthguardians.app.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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
    fun `bare official host url with query routes to section`() {
        val url = "https://earthguardians.org?utm_source=app"
        assertEquals("section?u=${Deeplinks.encode(url)}", Deeplinks.parseRoute(url))
    }

    @Test
    fun `raw nested query and fragment survive page argument routing`() {
        val pageUrl = "https://www.earthguardians.org/programs/youth%20climate?source=app&topic=climate+justice#join"
        val cases = listOf("section" to "u", "post" to "u", "event" to "e")

        cases.forEach { (target, key) ->
            val marker = "$target?$key="
            val expectedRoute = "$marker${Deeplinks.encode(pageUrl)}"
            val rawRoute = Deeplinks.parseRoute("earthguardians://$target?source=share&$key=$pageUrl")
            assertEquals(expectedRoute, rawRoute)
            // AndroidX Navigation decodes this route query argument once.
            assertEquals(pageUrl, java.net.URLDecoder.decode(rawRoute!!.removePrefix(marker), "UTF-8"))

            val encodedRoute = Deeplinks.parseRoute("earthguardians://$target?$key=${Deeplinks.encode(pageUrl)}")
            assertEquals(expectedRoute, encodedRoute)
            assertEquals(pageUrl, java.net.URLDecoder.decode(encodedRoute!!.removePrefix(marker), "UTF-8"))
        }
    }

    @Test
    fun `official app link filter has no path restriction so bare host matches`() {
        val manifest = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
        ).firstOrNull(File::isFile) ?: error("AndroidManifest.xml not found from ${File(".").absolutePath}")
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(manifest)
        val intentFilters = document.getElementsByTagName("intent-filter")
        val officialHosts = setOf("earthguardians.org", "www.earthguardians.org")
        val appLinkFilters = (0 until intentFilters.length)
            .map { intentFilters.item(it) as Element }
            .filter { filter ->
                val data = filter.getElementsByTagName("data")
                (0 until data.length).any { (data.item(it) as Element).getAttributeNS(androidNamespace, "host") in officialHosts }
            }

        assertEquals("Expected one official HTTPS link filter", 1, appLinkFilters.size)
        val data = appLinkFilters.single().getElementsByTagName("data")
        val hosts = (0 until data.length).map { (data.item(it) as Element).getAttributeNS(androidNamespace, "host") }.toSet()
        assertEquals(officialHosts, hosts)
        (0 until data.length).forEach { index ->
            val element = data.item(index) as Element
            assertEquals("https", element.getAttributeNS(androidNamespace, "scheme"))
            listOf("path", "pathPrefix", "pathPattern", "pathAdvancedPattern", "pathSuffix").forEach { attribute ->
                assertFalse("Official link filter must not constrain $attribute", element.hasAttributeNS(androidNamespace, attribute))
            }
        }
    }

    @Test
    fun `section page argument preserves nested query and fragment`() {
        val pageUrl = "https://www.earthguardians.org/programs/youth%20climate?source=app&topic=climate%20justice#join"
        val encodedUrl = Deeplinks.encode(pageUrl)
        val route = Deeplinks.parseRoute("earthguardians://section?u=$encodedUrl")

        assertEquals("section?u=$encodedUrl", route)
        assertEquals(pageUrl, Deeplinks.decode(route!!.substringAfter("section?u=")))
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
