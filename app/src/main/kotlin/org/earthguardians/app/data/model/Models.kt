package org.earthguardians.app.data.model

import kotlinx.serialization.Serializable

/** URL roots of the Earth Guardians ecosystem. */
object SitePaths {
    const val SITE_URL = "https://www.earthguardians.org"
    const val SITEMAP_URL = "$SITE_URL/sitemap.xml"

    /** Squarespace exposes RSS per collection page. */
    const val BLOG_FEED_RSS = "$SITE_URL/earthguardian-blog?format=RSS"
    const val EVENTS_FEED_RSS = "$SITE_URL/events?format=RSS"
}

/** One upcoming or past event from the EG events feed. */
@Serializable
data class EgEvent(
    val url: String,
    val title: String,
    val imageUrl: String? = null,
    val startsAt: Long? = null,
    val location: String? = null,
    val description: String? = null,
)

/** One story/blog post from the Earth Guardian blog. */
@Serializable
data class EgPost(
    val url: String,
    val title: String,
    val imageUrl: String? = null,
    val publishedAt: Long? = null,
    val teaser: String? = null,
    val contentHtml: String? = null,
)

@Serializable
data class EgFeed(
    val events: List<EgEvent> = emptyList(),
    val posts: List<EgPost> = emptyList(),
)

@Serializable
data class FeedCache(
    val fetchedAt: Long = 0L,
    val feed: EgFeed = EgFeed(),
)

@Serializable
data class SitemapCache(
    val fetchedAt: Long = 0L,
    val urls: List<String> = emptyList(),
)

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(val data: EgFeed, val isStale: Boolean = false) : FeedUiState
    data class Failed(val message: String) : FeedUiState
}

sealed interface SitemapUiState {
    data object Loading : SitemapUiState
    data class Ready(val urls: List<String>, val isStale: Boolean = false) : SitemapUiState
    data class Failed(val message: String) : SitemapUiState
}

/** Curated accent for catalog tiles — red = action, green = nature, neutral = information. */
enum class SectionAccent { Neutral, Red, Green }

/** Which part of the iOS-style bento the section belongs to. */
enum class SectionGroup(val title: String) {
    About("Who We Are"),
    People("Our People"),
    Involve("Get Involved"),
    Explore("Programs & Maps"),
    Resources("Resources"),
    Utility("More"),
}

/** A curated entry of the earthguardians.org site, integrated into the app. */
data class SiteSection(
    val title: String,
    val subtitle: String,
    val path: String,
    val group: SectionGroup,
    val accent: SectionAccent = SectionAccent.Neutral,
) {
    val url: String get() = "${SitePaths.SITE_URL}/$path"
}
