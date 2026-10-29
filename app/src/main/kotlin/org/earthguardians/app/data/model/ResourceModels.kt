package org.earthguardians.app.data.model

import kotlinx.serialization.Serializable

/** High-level subject used by native Resource Library filtering. */
@Serializable
enum class ResourceCategory {
    Community,
    Funding,
    ProjectGuides,
    Calendar,
    Learning,
    Campaigns,
    Programs,
    Reference,
}

/** Whether the canonical URL points to an exact page or to its verified parent collection. */
@Serializable
enum class ResourceLinkScope {
    DirectPage,
    CollectionFallback,
}

/** A source-level signal, kept separate from the resource's computed freshness state. */
@Serializable
enum class ResourceFreshnessHint {
    Reviewed,
    TimeSensitive,
    ReviewRecommended,
}

/** Freshness at a caller-provided time; passing time in keeps this core deterministic and testable. */
@Serializable
enum class ResourceFreshnessStatus {
    Current,
    TimeSensitive,
    ReviewRecommended,
    OpportunityExpired,
}

/**
 * Provenance for an indexed link. Timestamps are UTC epoch milliseconds. The review interval is
 * metadata maintenance policy, not a claim that the linked source changes on that schedule.
 */
@Serializable
data class ResourceSourceMetadata(
    val canonicalUrl: String,
    val sourceUrl: String,
    val reviewedAtEpochMillis: Long,
    val reviewIntervalDays: Int = DEFAULT_REVIEW_INTERVAL_DAYS,
    val freshnessHint: ResourceFreshnessHint = ResourceFreshnessHint.Reviewed,
    /** Expiry of a specific opportunity mentioned by the source, not of the page itself. */
    val opportunityEndsAtEpochMillis: Long? = null,
) {
    fun freshnessAt(asOfEpochMillis: Long): ResourceFreshnessStatus {
        if (opportunityEndsAtEpochMillis?.let { asOfEpochMillis >= it } == true) {
            return ResourceFreshnessStatus.OpportunityExpired
        }
        if (freshnessHint == ResourceFreshnessHint.ReviewRecommended) {
            return ResourceFreshnessStatus.ReviewRecommended
        }
        val reviewDueAt = reviewedAtEpochMillis + reviewIntervalDays * MILLIS_PER_DAY
        if (asOfEpochMillis >= reviewDueAt) return ResourceFreshnessStatus.ReviewRecommended
        if (freshnessHint == ResourceFreshnessHint.TimeSensitive) {
            return ResourceFreshnessStatus.TimeSensitive
        }
        return ResourceFreshnessStatus.Current
    }

    private companion object {
        const val DEFAULT_REVIEW_INTERVAL_DAYS = 180
        const val MILLIS_PER_DAY = 86_400_000L
    }
}

/**
 * Collection-level guide facts from Choose Action Now. These levels and time estimates are not
 * assigned to individual guide topics because those per-guide values were not verified.
 */
@Serializable
data class ResourceGuideMetadata(
    val difficultyLevels: List<Int>,
    val providesTimeEstimates: Boolean,
) {
    init {
        require(difficultyLevels.isNotEmpty() && difficultyLevels.all { it in 1..3 })
        require(difficultyLevels == difficultyLevels.distinct().sorted())
    }
}

/** A metadata-only resource record; editorial text and third-party assets are never bundled here. */
@Serializable
data class LibraryResource(
    val id: String,
    val title: String,
    val category: ResourceCategory,
    val source: ResourceSourceMetadata,
    val linkScope: ResourceLinkScope = ResourceLinkScope.DirectPage,
    /** Search labels only; not summaries or copied source text. */
    val searchTerms: List<String> = emptyList(),
    val guideMetadata: ResourceGuideMetadata? = null,
) {
    val canonicalUrl: String get() = source.canonicalUrl
}
