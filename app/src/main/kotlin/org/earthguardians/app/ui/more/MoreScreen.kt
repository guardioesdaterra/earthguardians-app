package org.earthguardians.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import org.earthguardians.app.ui.design.EgSearchField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.earthguardians.app.data.model.SectionGroup
import org.earthguardians.app.data.model.SiteSection
import org.earthguardians.app.data.repo.SiteCatalog
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.design.EgPill
import org.earthguardians.app.ui.design.EmptyState
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.LargeTitle
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.egPalette

/**
 * More: every single page of earthguardians.org integrated — curated groups
 * from SiteCatalog (mapped from /sitemap.xml) plus a fully dynamic index of
 * any new pages discovered live.
 */
@Composable
internal fun MoreScreen(
    graph: AppGraph,
    openSection: (SiteSection) -> Unit,
    openSettings: () -> Unit,
    openNotifications: () -> Unit,
    openUrl: (String) -> Unit,
) {
    val palette = egPalette
    var query by remember { mutableStateOf("") }
    val sitemap by graph.content.sitemap.collectAsState()

    val filtered = remember(query) {
        SiteCatalog.sections.filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LargeTitle(title = "More", modifier = Modifier.weight(1f))
                    HeaderTile(false) { openNotifications() }
                    HeaderTile(true) { openSettings() }
                }
                EgSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search the whole site…",
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No matches",
                    message = "Try a different phrase — everything on earthguardians.org is in here.",
                )
            }
        } else {
            SectionGroup.entries.forEach { group ->
                val groupSections = filtered.filter { it.group == group }
                if (groupSections.isNotEmpty()) {
                    item {
                        GroupCard(
                            title = group.title,
                            sections = groupSections,
                            accent = when (group) {
                                SectionGroup.Involve -> PillTone.Red
                                SectionGroup.Explore -> PillTone.Green
                                else -> PillTone.Neutral
                            },
                            onOpen = openSection,
                        )
                    }
                }
            }
        }

        // ── Fully dynamic sitemap index ────────────────────────────
        item {
            when (val sm = sitemap) {
                is org.earthguardians.app.data.model.SitemapUiState.Ready -> {
                    val curatedPaths = SiteCatalog.sections.map { it.path }.toSet()
                    val extras = sm.urls
                        .mapNotNull { url ->
                            url.removePrefix("https://www.earthguardians.org/").trim('/').takeIf { it.isNotBlank() }
                        }
                        .filter { it !in curatedPaths && !it.contains("/") } // top-level pages only
                        .distinct()
                        .sorted()
                        .take(160)
                    if (extras.isNotEmpty()) {
                        SitemapIndexCard(extras = extras, openUrl = openUrl)
                    } else {
                        EgPill(text = "sitemap synced", tone = PillTone.Green)
                    }
                }
                else -> EgPill(text = "discovering new pages from sitemap…", tone = PillTone.Neutral)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Everything on earthguardians.org, in your pocket.",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun HeaderTile(isSettings: Boolean, onClick: () -> Unit) {
    val palette = egPalette
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(40.dp)
            .clip(shape)
            .background(palette.glass, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSettings) Icons.Rounded.Settings else Icons.Rounded.Notifications,
            contentDescription = if (isSettings) "Settings" else "Notifications",
            tint = palette.onBackground,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun GroupCard(
    title: String,
    accent: PillTone,
    sections: List<SiteSection>,
    onOpen: (SiteSection) -> Unit,
) {
    val palette = egPalette
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            EgPill(text = "${sections.size} pages", tone = accent)
        }
        sections.forEach { section ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpen(section) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        color = palette.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (section.subtitle.isNotBlank()) {
                        Text(
                            text = section.subtitle,
                            color = palette.muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = palette.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SitemapIndexCard(
    extras: List<String>,
    openUrl: (String) -> Unit,
) {
    val palette = egPalette
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Website index",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            EgPill(text = "from /sitemap.xml", tone = PillTone.Green)
        }
        Text(
            text = "Live-discovered pages that haven't curated placement yet — included automatically.",
            color = palette.muted,
            fontSize = 12.sp,
        )
        extras.forEach { path ->
            val label = path.replace('-', ' ')
                .replaceFirstChar { c -> c.uppercaseChar() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { openUrl("https://www.earthguardians.org/$path") }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = palette.onBackground,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = palette.muted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
