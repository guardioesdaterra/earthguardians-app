package org.earthguardians.app.data.repo

import org.earthguardians.app.data.model.ResourceCategory
import org.earthguardians.app.data.model.ResourceFreshnessHint
import org.earthguardians.app.data.model.ResourceFreshnessStatus
import org.earthguardians.app.data.model.ResourceLinkScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceLibraryTest {
    @Test
    fun `catalog ids are unique and every record carries official source metadata`() {
        val ids = ResourceLibrary.resources.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        ResourceLibrary.resources.forEach { item ->
            assertTrue(item.id.isNotBlank())
            assertTrue(item.title.isNotBlank())
            assertTrue(item.source.canonicalUrl.startsWith("https://www.earthguardians.org/"))
            assertTrue(item.source.sourceUrl.startsWith("https://www.earthguardians.org/"))
            assertTrue(item.source.reviewedAtEpochMillis > 0L)
        }
    }

    @Test
    fun `unresolved third party links explicitly fall back to an official collection`() {
        val fallbackItems = ResourceLibrary.resources.filter {
            it.linkScope == ResourceLinkScope.CollectionFallback
        }
        assertTrue(fallbackItems.isNotEmpty())
        fallbackItems.forEach { item ->
            assertEquals("${SiteLibraryUrl.ROOT}/eg-resources", item.canonicalUrl)
        }
    }

    @Test
    fun `guide levels and estimates are collection facts not per-topic claims`() {
        val guides = ResourceLibrary.byId("project-guides-toolkits")
        assertNotNull(guides?.guideMetadata)
        assertEquals(listOf(1, 2, 3), guides?.guideMetadata?.difficultyLevels)
        assertTrue(guides?.guideMetadata?.providesTimeEstimates == true)
        assertTrue(ResourceLibrary.resources.filter { it.id != "project-guides-toolkits" }
            .all { it.guideMetadata == null })
    }

    @Test
    fun `dated grants and events are flagged for review`() {
        listOf("project-grants", "events-calendar").forEach { id ->
            val item = ResourceLibrary.byId(id)!!
            assertEquals(ResourceFreshnessHint.ReviewRecommended, item.source.freshnessHint)
            assertEquals(
                ResourceFreshnessStatus.ReviewRecommended,
                item.source.freshnessAt(ResourceLibrary.REVIEWED_AT_EPOCH_MILLIS),
            )
        }
    }

    @Test
    fun `expired training deadline does not imply the linked page is gone`() {
        val iyi = ResourceLibrary.byId("indigenous-youth-initiative")!!
        assertEquals(ResourceFreshnessHint.TimeSensitive, iyi.source.freshnessHint)
        assertEquals(
            ResourceFreshnessStatus.TimeSensitive,
            iyi.source.freshnessAt(1_789_689_599_999L),
        )
        assertEquals(
            ResourceFreshnessStatus.OpportunityExpired,
            iyi.source.freshnessAt(1_789_689_600_000L),
        )
        assertTrue(iyi.canonicalUrl.endsWith("/indigenous-youth-initiative"))
    }

    @Test
    fun `search matches collection labels and supports native filters`() {
        assertEquals(
            listOf("project-guides-toolkits"),
            ResourceLibrary.search("  FARMERS  ", ResourceCategory.ProjectGuides).map { it.id },
        )
        assertEquals(
            listOf("project-guides-toolkits"),
            ResourceLibrary.search(difficultyLevel = 2).map { it.id },
        )
        assertEquals(emptyList<Any>(), ResourceLibrary.search(difficultyLevel = 4))
        assertEquals("crews", ResourceLibrary.byId("crews")?.id)
        assertNull(ResourceLibrary.byId("unknown"))
    }

    @Test
    fun `catalog stores no editorial summaries or media payloads`() {
        assertFalse(ResourceLibrary.resources.any { it.searchTerms.any(String::isBlank) })
        // The records consist of URLs, labels and metadata; media/content fields are intentionally absent.
        assertTrue(ResourceLibrary.resources.none { it.source.canonicalUrl.contains("drive.google.com") })
    }
}

private object SiteLibraryUrl {
    const val ROOT = "https://www.earthguardians.org"
}
