package org.earthguardians.app.ui.common

import org.earthguardians.app.R

/** Owner-approved photographs from the official Earth Guardians homepage. */
internal object OfficialSiteImages {
    val COMMUNITY = R.drawable.eg_home_community
    val BENIN_ACTION = R.drawable.eg_home_benin_action
    val LEAVES = R.drawable.eg_home_leaves
    val FIELDWORK = R.drawable.eg_home_fieldwork
    val YOUTH_GATHERING = R.drawable.eg_home_youth_gathering
    val CLIMATE_ACTION = R.drawable.eg_home_climate_action

    val COLLAGE = listOf(COMMUNITY, BENIN_ACTION, LEAVES, FIELDWORK, YOUTH_GATHERING, CLIMATE_ACTION)

    fun featuredEvent(url: String): Int? = when {
        url.substringBefore('?').trimEnd('/').endsWith("/events/2025/6/14/project-grant-summit") ->
            R.drawable.eg_feed_event_project_grant_summit
        url.substringBefore('?').trimEnd('/').endsWith("/events/2024/4/28/master-class-74kbc-r8zhc-7pwbe-9dc8n") ->
            R.drawable.eg_feed_event_master_class
        else -> null
    }

    fun featuredStory(url: String): Int? = when {
        url.substringBefore('?').trimEnd('/').endsWith("/earthguardian-blog/2025/10/27/sptk-series-world-water-day-glaciers") ->
            R.drawable.eg_feed_story_world_water_day_glaciers
        else -> null
    }
}
