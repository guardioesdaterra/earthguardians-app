package org.earthguardians.app.ui.events

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.earthguardians.app.data.model.EgEvent
import org.earthguardians.app.data.model.FeedUiState
import org.earthguardians.app.data.parse.RssParser
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.common.EgImage
import org.earthguardians.app.ui.design.EgSearchField
import org.earthguardians.app.ui.design.EgPill
import org.earthguardians.app.ui.design.EmptyState
import org.earthguardians.app.ui.design.FailedState
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.LargeTitle
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.design.ShimmerCard
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.theme.egPalette

/** Events tab: searchable archive of crew events from the live feed. */
@Composable
internal fun EventsScreen(
    graph: AppGraph,
    openEvent: (EgEvent) -> Unit,
) {
    val palette = egPalette
    val feed by graph.content.feed.collectAsState()
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LargeTitle(title = "Events", subtitle = "Every crew gathering, live from the site feed.")
            EgSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search events…",
            )
        }

        when (val state = feed) {
            is FeedUiState.Loading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(4) { ShimmerCard() }
            }

            is FeedUiState.Failed -> Box(Modifier.padding(top = 12.dp)) {
                FailedState(
                    message = state.message,
                    onRetry = { scope.launch { graph.content.refreshFeed(force = true) } },
                )
            }

            is FeedUiState.Ready -> {
                val events = remember(state, query) {
                    state.data.events
                        .sortedByDescending { it.startsAt ?: 0L }
                        .filter { it.title.contains(query.trim(), ignoreCase = true) || query.isBlank() }
                }
                if (events.isEmpty()) {
                    Box(Modifier.padding(top = 12.dp)) {
                        EmptyState(
                            icon = Icons.Rounded.Event,
                            title = "Nothing here (yet)",
                            message = "No events matched your search. Check back soon — crews post constantly.",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(events, key = { it.url }) { event ->
                            EventRowCard(event = event, onClick = { openEvent(event) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRowCard(
    event: EgEvent,
    onClick: () -> Unit,
) {
    val palette = egPalette
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EgImage(
                url = event.imageUrl,
                modifier = Modifier.size(92.dp),
                cornerRadius = 20.dp,
                contentDescription = event.title,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EgPill(text = RssParser.formatEpochShort(event.startsAt), tone = PillTone.Green)
                Text(
                    text = event.title,
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!event.location.isNullOrBlank()) {
                    Text(
                        text = event.location,
                        color = palette.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Leftover shared detail action row used by [EventDetailScreen]. */
@Composable
internal fun DetailButtonRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}
