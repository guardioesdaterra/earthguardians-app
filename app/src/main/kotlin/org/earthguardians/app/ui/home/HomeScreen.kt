package org.earthguardians.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.earthguardians.app.data.model.EgEvent
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.model.FeedUiState
import org.earthguardians.app.data.model.SiteSection
import org.earthguardians.app.data.parse.RssParser
import org.earthguardians.app.data.repo.SiteCatalog
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.common.EgImage
import org.earthguardians.app.ui.common.PostRowCard
import org.earthguardians.app.ui.design.BentoTile
import org.earthguardians.app.ui.design.EgPill
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.FailedState
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.design.SectionTitle
import org.earthguardians.app.ui.design.ShimmerCard
import org.earthguardians.app.ui.design.StatTile
import org.earthguardians.app.ui.root.Routes
import org.earthguardians.app.ui.theme.egPalette

/**
 * Home: large-title header, mission hero, upcoming events carousel, stories,
 * impact stats bento and the "make it happen" action grid — all glass, all
 * black/white/red/green.
 */
@Composable
internal fun HomeScreen(
    graph: AppGraph,
    navigateToTab: (String) -> Unit,
    openSettings: () -> Unit,
    openNotifications: () -> Unit,
    openEvent: (EgEvent) -> Unit,
    openStory: (EgPost) -> Unit,
    openSection: (SiteSection) -> Unit,
) {
    val palette = egPalette
    val feed by graph.content.feed.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(palette.accent, palette.eco))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("EG", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "EARTH GUARDIANS",
                    color = palette.onBackground,
                    fontSize = 12.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                HeaderIconButton(Icons.Rounded.Notifications, "Notifications", openNotifications)
                HeaderIconButton(Icons.Rounded.Settings, "Settings", openSettings)
            }
        }

        // ── Mission hero ───────────────────────────────────────────
        item {
            GlassCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                EgPill(text = "for youth · by youth", tone = PillTone.Green)
                Text(
                    text = "“You can be the single drop of water that creates an enormous ripple effect.”",
                    color = palette.onBackground,
                    fontSize = 22.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    text = "Emmy Scott · Executive Director",
                    color = palette.muted,
                    fontSize = 13.sp,
                )
            }
        }

        // ── Content state ──────────────────────────────────────────
        when (val state = feed) {
            is FeedUiState.Loading -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ShimmerCard(height = 170.dp)
                    ShimmerCard()
                    ShimmerCard()
                }
            }

            is FeedUiState.Failed -> item {
                FailedState(
                    message = state.message,
                    modifier = Modifier.fillMaxWidth(),
                    onRetry = {
                        scope.launch {
                            graph.content.refreshFeed(force = true)
                            graph.content.refreshSitemap()
                        }
                    },
                )
            }

            is FeedUiState.Ready -> {
                if (state.isStale) {
                    item {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            EgPill(text = "offline · showing cached content", tone = PillTone.Red)
                        }
                    }
                }

                // Upcoming events carousel
                item {
                    SectionTitle(
                        title = "Upcoming",
                        action = "See all",
                        onAction = { navigateToTab(Routes.EVENTS) },
                    )
                }
                if (state.data.events.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(state.data.events.take(8)) { event ->
                                EventCarouselCard(event = event, onClick = { openEvent(event) })
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "No events on the calendar right now — the full archive lives under Events.",
                            color = palette.muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }

                // Stories
                if (state.data.posts.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Stories",
                            action = "See all",
                            onAction = { navigateToTab(Routes.STORIES) },
                        )
                    }
                    items(state.data.posts.take(5)) { post ->
                        PostRowCard(
                            post = post,
                            modifier = Modifier.padding(horizontal = 14.dp),
                            onClick = { openStory(post) },
                        )
                    }
                }
            }
        }

        // ── Impact stats ───────────────────────────────────────────
        item {
            SectionTitle(title = "The receipts")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(value = "1M+", label = "people impacted", tone = PillTone.Green, modifier = Modifier.weight(1f))
                StatTile(value = "20K+", label = "trees planted", tone = PillTone.Green, modifier = Modifier.weight(1f))
                StatTile(value = "100%", label = "youth-led", tone = PillTone.Red, modifier = Modifier.weight(1f))
                StatTile(value = "94%", label = "youth & BIPOC staff", tone = PillTone.Neutral, modifier = Modifier.weight(1f))
            }
        }

        // ── Make it happen ─────────────────────────────────────────
        item {
            SectionTitle(title = "Make it happen")
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        title = "Take action",
                        subtitle = "Start with what's in your hands",
                        icon = Icons.Rounded.Campaign,
                        tone = PillTone.Red,
                        modifier = Modifier.weight(1f),
                        openSection = openSection,
                    )
                    ActionTile(
                        title = "Join a crew",
                        subtitle = "Find or launch your circle",
                        icon = Icons.Rounded.Groups,
                        tone = PillTone.Green,
                        modifier = Modifier.weight(1f),
                        openSection = openSection,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(
                        title = "EG-Maps",
                        subtitle = "The atlas of the movement",
                        icon = Icons.Rounded.Map,
                        tone = PillTone.Green,
                        modifier = Modifier.weight(1f),
                        openSection = openSection,
                    )
                    ActionTile(
                        title = "Donate",
                        subtitle = "Fuel youth climate work",
                        icon = Icons.Rounded.Favorite,
                        tone = PillTone.Red,
                        modifier = Modifier.weight(1f),
                        openSection = openSection,
                    )
                }
            }
        }

        // ── Footer ─────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Earth Guardians · reachout@earthguardians.org",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
                Text(
                    text = "PO Box 1561 Laurel, MD 20725 · EIN 84-1397083",
                    color = palette.muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val palette = egPalette
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(palette.glass)
            .egTap { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = palette.onBackground, modifier = Modifier.size(19.dp))
    }
}

private fun Modifier.egTap(onClick: () -> Unit): Modifier = this.clickable { onClick() }

/** Bento tile wired to a concrete catalog section. */
@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tone: PillTone,
    modifier: Modifier = Modifier,
    openSection: (SiteSection) -> Unit,
) {
    BentoTile(
        title = title,
        subtitle = subtitle,
        icon = icon,
        tone = tone,
        modifier = modifier,
        onClick = {
            SiteCatalog.byPath(
                when (title) {
                    "Take action" -> "take-action"
                    "Join a crew" -> "crews"
                    "EG-Maps" -> "eg-maps"
                    else -> "donate"
                },
            )?.let(openSection)
        },
    )
}

/** Horizontal carousel card for an event. */
@Composable
private fun EventCarouselCard(
    event: EgEvent,
    onClick: () -> Unit,
) {
    val palette = egPalette
    val shape = RoundedCornerShape(EgRadius.card)
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(shape)
            .background(palette.glass, shape)
            .egTap(onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            EgImage(
                url = event.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                cornerRadius = 18.dp,
                contentDescription = event.title,
            )
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                EgPill(text = RssParser.formatEpochShort(event.startsAt), tone = PillTone.Neutral)
            }
        }
        Text(
            text = event.title,
            color = palette.onBackground,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp),
        )
    }
}
