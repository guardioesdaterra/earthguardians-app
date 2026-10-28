package org.earthguardians.app.ui.events

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.earthguardians.app.data.model.EgEvent
import org.earthguardians.app.data.model.FeedUiState
import org.earthguardians.app.data.parse.RssParser
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.common.EgImage
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.design.EmptyState
import org.earthguardians.app.ui.design.EgPill
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.egPalette

/**
 * Event detail: hero image, real date, description and native actions
 * (system calendar intent, share, open the event page in-app).
 */
@Composable
internal fun EventDetailScreen(
    graph: AppGraph,
    eventUrl: String,
    onBack: () -> Unit,
    onOpenPage: (String) -> Unit,
) {
    val palette = egPalette
    val feed by graph.content.feed.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val event = (feed as? FeedUiState.Ready)?.data?.events?.firstOrNull { it.url == eventUrl }

    LazyColumn(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 30.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    event?.let { e ->
                        runCatching {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${e.title} — ${e.url}")
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = palette.onBackground, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (event == null) {
            item {
                Box(Modifier.padding(top = 30.dp)) {
                    EmptyState(
                        icon = Icons.Rounded.OpenInNew,
                        title = "Event unavailable",
                        message = "This event fell out of the latest feed sync. Try refreshing from the Events tab.",
                    )
                }
            }
        } else {
            item {
                EgImage(
                    url = event.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 16.dp),
                    cornerRadius = 26.dp,
                    contentDescription = event.title,
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EgPill(text = RssParser.formatEpochShort(event.startsAt), tone = PillTone.Green)
                        event.location?.let { EgPill(text = it, tone = PillTone.Neutral) }
                    }
                    Text(
                        text = event.title,
                        color = palette.onBackground,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.4).sp,
                    )
                    if (!event.description.isNullOrBlank()) {
                        Text(
                            text = event.description,
                            color = palette.muted,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EgPrimaryButton(
                            label = "Save to calendar",
                            tone = PillTone.Green,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                runCatching {
                                    val insert = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, event.title)
                                        event.startsAt?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
                                    }
                                    context.startActivity(insert)
                                }
                            },
                        )
                        EgPrimaryButton(
                            label = "Open page",
                            tone = PillTone.Red,
                            leading = Icons.Rounded.OpenInNew,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenPage(event.url) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Crew events are hosted by Earth Guardians regional councils. Reach out on the event page for Zoom links, sign ups and volunteer slots.",
                            color = palette.muted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }
        }
    }
}
