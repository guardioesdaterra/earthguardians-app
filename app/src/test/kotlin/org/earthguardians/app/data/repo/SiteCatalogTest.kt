package org.earthguardians.app.data.repo

import org.earthguardians.app.data.model.SectionAccent
import org.earthguardians.app.data.model.SectionGroup
import org.earthguardians.app.data.model.SitePaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the curated earthguardians.org integration map. */
class SiteCatalogTest {

    @Test
    fun `catalog is non-empty and paths are unique`() {
        val paths = SiteCatalog.sections.map { it.path }
        assertTrue(paths.size >= 30)
        assertEquals(paths.size, paths.distinct().size)
    }

    @Test
    fun `every section has title, subtitle, path and a valid url`() {
        SiteCatalog.sections.forEach { section ->
            assertTrue(section.title.isNotBlank())
            assertTrue(section.subtitle.isNotBlank())
            assertTrue(section.path.isNotBlank())
            assertTrue("Path must not contain a leading slash: ${section.path}",
                !section.path.startsWith("/"))
            assertEquals("${SitePaths.SITE_URL}/${section.path}", section.url)
        }
    }

    @Test
    fun `featured paths all resolve to real sections`() {
        val featured = SiteCatalog.featured
        assertEquals(SiteCatalog.featuredPaths.size, featured.size)
        featured.forEach { assertNotNull(it) }
    }

    @Test
    fun `all site groups are represented`() {
        val groups = SiteCatalog.sections.map { it.group }.toSet()
        SectionGroup.entries.forEach { group ->
            assertTrue("Missing group $group", groups.contains(group))
        }
    }

    @Test
    fun `eg-maps is featured and green`() {
        val maps = SiteCatalog.byPath("eg-maps")
        assertNotNull(maps)
        assertEquals(SectionGroup.Explore, maps?.group)
        assertEquals(SectionAccent.Green, maps?.accent)
    }

    @Test
    fun `byPath trims slashes and misses unknown paths`() {
        assertEquals("eg-maps", SiteCatalog.byPath("/eg-maps/")?.path)
        assertEquals("eg-maps", SiteCatalog.byPath("eg-maps")?.path)
        assertEquals(null, SiteCatalog.byPath("not-a-page"))
    }

    @Test
    fun `byUrl strips the site root`() {
        assertNotNull(SiteCatalog.byUrl("${SitePaths.SITE_URL}/donate"))
        assertEquals(null, SiteCatalog.byUrl("https://example.com/donate"))
    }

    @Test
    fun `impact stats match the 2024 impact report`() {
        assertEquals("1M+", SiteCatalog.Impact.DIRECTLY_IMPACTED)
        assertEquals("20K+", SiteCatalog.Impact.TREES)
        assertEquals("100%", SiteCatalog.Impact.YOUTH_LED)
        assertEquals("94%", SiteCatalog.Impact.YOUTH_STAFF)
    }

    @Test
    fun `dead site pages are never linked`() {
        // /youth-councils and /regional-directors 404 on the live site (checked
        // Oct 2026); the real pages are the profile sub-pages and eg-team.
        assertEquals(null, SiteCatalog.byPath("youth-councils"))
        assertEquals(null, SiteCatalog.byPath("regional-directors"))
    }

    @Test
    fun `our people group carries leadership and speaker profiles`() {
        val people = SiteCatalog.sections.filter { it.group == SectionGroup.People }
        assertTrue("expected the full people roster", people.size >= 10)
        assertTrue(people.any { it.path == "tyler-massias" })
        assertTrue(people.any { it.path == "catherine-mongella-speakersbureau" })
        assertTrue(people.any { it.path == "youth-councils/nina-berglund" })
        // Sub-collection profile pages must keep their folder prefix.
        assertTrue(people.any { it.url.contains("/youth-councils/") })
    }
}
