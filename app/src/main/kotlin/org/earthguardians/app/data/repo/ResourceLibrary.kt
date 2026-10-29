package org.earthguardians.app.data.repo

import org.earthguardians.app.data.model.LibraryResource
import org.earthguardians.app.data.model.ResourceCategory
import org.earthguardians.app.data.model.ResourceFreshnessHint
import org.earthguardians.app.data.model.ResourceGuideMetadata
import org.earthguardians.app.data.model.ResourceLinkScope
import org.earthguardians.app.data.model.ResourceSourceMetadata
import org.earthguardians.app.data.model.SitePaths
import java.util.Locale

/**
 * Curated metadata index for the official Earth Guardians Resource Library.
 * Keep source pages and direct destinations distinct; unresolved PDF/Drive/YouTube links point to
 * the verified collection page rather than to guessed or scraped external URLs.
 */
object ResourceLibrary {
    /** Date the supplied official-site research was reviewed: 2026-10-29 UTC. */
    const val REVIEWED_AT_EPOCH_MILLIS = 1_793_232_000_000L
    private const val REVIEW_INTERVAL_DAYS = 180

    val resources: List<LibraryResource> = listOf(
        resource(
            id = "resource-library",
            title = "Resource Library",
            category = ResourceCategory.Reference,
            canonicalPath = "eg-resources",
            searchTerms = listOf("resources", "lessons", "videos", "toolkits", "guides", "pdf", "drive", "docs"),
        ),
        resource(
            id = "crews",
            title = "Crews",
            category = ResourceCategory.Community,
            canonicalPath = "crews",
            searchTerms = listOf("signup", "onboarding", "mentorship", "regional crews counselor", "interactive maps"),
        ),
        resource(
            id = "project-grants",
            title = "Project Grants",
            category = ResourceCategory.Funding,
            canonicalPath = "project-grants",
            freshnessHint = ResourceFreshnessHint.ReviewRecommended,
            searchTerms = listOf("microgrants", "application", "crew funding", "closed application"),
        ),
        resource(
            id = "project-guides-toolkits",
            title = "Project Guides & Toolkits",
            category = ResourceCategory.ProjectGuides,
            canonicalPath = "eg-projectguides-toolkits",
            guideMetadata = ResourceGuideMetadata(
                difficultyLevels = listOf(1, 2, 3),
                providesTimeEstimates = true,
            ),
            searchTerms = listOf(
                "choose action now", "climate-friendly banking", "farmers markets", "lobbying",
                "legislation", "community fridges", "free markets", "food forests",
                "guerilla gardening", "native planting", "cleanups", "art activism",
                "nonviolent direct action",
            ),
        ),
        resource(
            id = "events-calendar",
            title = "Events",
            category = ResourceCategory.Calendar,
            canonicalPath = "events",
            freshnessHint = ResourceFreshnessHint.ReviewRecommended,
            searchTerms = listOf("calendar", "upcoming", "event", "ics", "google calendar"),
        ),
        resource(
            id = "youth-video-lessons",
            title = "Youth Lessons & Videos",
            category = ResourceCategory.Learning,
            canonicalPath = "eg-resources",
            sourcePath = "eg-resources",
            linkScope = ResourceLinkScope.CollectionFallback,
            searchTerms = listOf("youtube", "video", "lesson", "lesson plans", "youth education"),
        ),
        resource(
            id = "youth-written-blog",
            title = "Youth-Written Blog",
            category = ResourceCategory.Learning,
            canonicalPath = "earthguardian-blog",
            sourcePath = "eg-resources",
            searchTerms = listOf("stories", "blog", "youth writing"),
        ),
        resource(
            id = "classroom-and-home-tools",
            title = "Classroom, Crew & Home Tools",
            category = ResourceCategory.Learning,
            canonicalPath = "eg-resources",
            sourcePath = "eg-resources",
            linkScope = ResourceLinkScope.CollectionFallback,
            searchTerms = listOf(
                "classroom", "school crew", "crew guides", "permaculture", "divestment",
                "50 simple things", "home environmental audit", "school environmental audit",
            ),
        ),
        resource(
            id = "campaigns",
            title = "Campaigns",
            category = ResourceCategory.Campaigns,
            canonicalPath = "campaigns-2",
            searchTerms = listOf("choose action now", "stop the money pipeline", "civic engagement", "divestment"),
        ),
        resource(
            id = "choose-action-now",
            title = "Choose Action Now",
            category = ResourceCategory.Campaigns,
            canonicalPath = "can",
            sourcePath = "campaigns-2",
            searchTerms = listOf("can", "youth global day of action"),
        ),
        resource(
            id = "stop-the-money-pipeline",
            title = "Stop the Money Pipeline",
            category = ResourceCategory.Campaigns,
            canonicalPath = "stmp",
            sourcePath = "campaigns-2",
            searchTerms = listOf("stmp", "divestment", "finance"),
        ),
        resource(
            id = "civic-engagement",
            title = "Civic Engagement",
            category = ResourceCategory.Campaigns,
            canonicalPath = "civic-engagement",
            sourcePath = "campaigns-2",
            searchTerms = listOf("voting", "legislation", "policy"),
        ),
        resource(
            id = "divest-and-defund",
            title = "Divest & Defund",
            category = ResourceCategory.Campaigns,
            canonicalPath = "divest-defund",
            sourcePath = "campaigns-2",
            searchTerms = listOf("divestment", "fossil fuels", "finance"),
        ),
        resource(
            id = "ej-principles",
            title = "Environmental Justice Principles",
            category = ResourceCategory.Reference,
            canonicalPath = "principles-of-environmental-justice",
            sourcePath = "eg-resources",
            searchTerms = listOf("environmental justice", "ej principles"),
        ),
        resource(
            id = "sustainable-development-goals",
            title = "Sustainable Development Goals",
            category = ResourceCategory.Reference,
            canonicalPath = "sdgs",
            sourcePath = "eg-resources",
            searchTerms = listOf("sdgs", "un sustainable development goals"),
        ),
        resource(
            id = "glossary",
            title = "Dictionary of Important Terms",
            category = ResourceCategory.Reference,
            canonicalPath = "dictionary-of-important-terms",
            sourcePath = "eg-resources",
            searchTerms = listOf("glossary", "intersectionality", "allyship", "place-based education"),
        ),
        resource(
            id = "speakers-bureau",
            title = "Speakers’ Bureau",
            category = ResourceCategory.Community,
            canonicalPath = "speakers-bureau",
            sourcePath = "eg-resources",
            searchTerms = listOf("speakers", "youth speakers", "booking"),
        ),
        resource(
            id = "programs-overview",
            title = "Programs",
            category = ResourceCategory.Programs,
            canonicalPath = "eg-programs",
            sourcePath = "eg-programs",
            searchTerms = listOf("crews", "rcc", "iy i", "speakers bureau"),
        ),
        resource(
            id = "indigenous-youth-initiative",
            title = "Indigenous Youth Initiative",
            category = ResourceCategory.Programs,
            canonicalPath = "indigenous-youth-initiative",
            sourcePath = "eg-programs",
            freshnessHint = ResourceFreshnessHint.TimeSensitive,
            opportunityEndsAtEpochMillis = 1_789_689_600_000L,
            searchTerms = listOf("iyi", "training", "october 2026"),
        ),
    )

    fun byId(id: String): LibraryResource? = resources.firstOrNull { it.id == id }

    /** Search by title, stable ID or short search labels, with optional category/difficulty filters. */
    fun search(
        query: String = "",
        category: ResourceCategory? = null,
        difficultyLevel: Int? = null,
    ): List<LibraryResource> {
        if (difficultyLevel != null && difficultyLevel !in 1..3) return emptyList()
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return resources.filter { item ->
            val matchesCategory = category == null || item.category == category
            val matchesDifficulty = difficultyLevel == null ||
                item.guideMetadata?.difficultyLevels?.contains(difficultyLevel) == true
            val matchesQuery = normalizedQuery.isEmpty() ||
                item.title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                item.id.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                item.searchTerms.any { it.lowercase(Locale.ROOT).contains(normalizedQuery) }
            matchesCategory && matchesDifficulty && matchesQuery
        }
    }

    private fun resource(
        id: String,
        title: String,
        category: ResourceCategory,
        canonicalPath: String,
        sourcePath: String = canonicalPath,
        linkScope: ResourceLinkScope = ResourceLinkScope.DirectPage,
        freshnessHint: ResourceFreshnessHint = ResourceFreshnessHint.Reviewed,
        opportunityEndsAtEpochMillis: Long? = null,
        guideMetadata: ResourceGuideMetadata? = null,
        searchTerms: List<String> = emptyList(),
    ): LibraryResource = LibraryResource(
        id = id,
        title = title,
        category = category,
        source = ResourceSourceMetadata(
            canonicalUrl = "${SitePaths.SITE_URL}/$canonicalPath",
            sourceUrl = "${SitePaths.SITE_URL}/$sourcePath",
            reviewedAtEpochMillis = REVIEWED_AT_EPOCH_MILLIS,
            reviewIntervalDays = REVIEW_INTERVAL_DAYS,
            freshnessHint = freshnessHint,
            opportunityEndsAtEpochMillis = opportunityEndsAtEpochMillis,
        ),
        linkScope = linkScope,
        searchTerms = searchTerms,
        guideMetadata = guideMetadata,
    )
}
