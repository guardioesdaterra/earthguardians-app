package org.earthguardians.app.ui.more

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.earthguardians.app.data.repo.StoredNotification
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.design.EmptyState
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.egPalette

/** In-app Notification Center — mirrors everything the app posted system-wide. */
@Composable
internal fun NotificationsScreen(
    graph: AppGraph,
    onBack: () -> Unit,
) {
    val palette = egPalette
    val items by graph.notifications.history.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    fun markAllRead() {
        scope.launch {
            graph.prefs.saveNotifications(items.map { it.copy(read = true) })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = palette.onBackground, modifier = Modifier.size(22.dp))
            }
            Text(
                text = "Notifications",
                color = palette.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp,
            )
        }

        if (items.isEmpty()) {
            Box(Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)) {
                EmptyState(
                    icon = Icons.Rounded.Notifications,
                    title = "All caught up",
                    message = "New events and stories will land here (and in your system notifications) as crews post them.",
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EgPrimaryButton(
                    label = "Mark all read",
                    tone = PillTone.Green,
                    modifier = Modifier.weight(1f),
                    onClick = { markAllRead() },
                )
                EgPrimaryButton(
                    label = "Clear",
                    tone = PillTone.Neutral,
                    modifier = Modifier.weight(1f),
                    onClick = { scope.launch { graph.prefs.saveNotifications(emptyList()) } },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items.size) { index ->
                    val record = items[index]
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, onClick = {
                        graph.routeBus.postRoute(record.route)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (record.read) palette.outline else palette.accent),
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = record.title,
                                    color = palette.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = record.message,
                                    color = palette.muted,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = DateUtils.getRelativeTimeSpanString(record.postedAt).toString(),
                                color = palette.muted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
