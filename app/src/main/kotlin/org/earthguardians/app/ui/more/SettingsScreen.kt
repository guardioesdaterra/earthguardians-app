package org.earthguardians.app.ui.more

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.earthguardians.app.BuildConfig
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.notifications.NotificationCoordinator
import org.earthguardians.app.ui.design.DisclosureRow
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.design.EgSegmentedControl
import org.earthguardians.app.ui.design.GlassCard
import org.earthguardians.app.ui.design.LargeTitle
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.ThemeMode
import org.earthguardians.app.ui.theme.egPalette

/** Settings: theme switcher, notification controls, about/integrity rows. */
@Composable
internal fun SettingsScreen(
    graph: AppGraph,
    onBack: () -> Unit,
) {
    val palette = egPalette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by graph.theme.mode.collectAsState(initial = ThemeMode.System)

    val modeOptions = ThemeMode.entries.map { it.label }
    val selectedMode = ThemeMode.entries.indexOf(mode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
            LargeTitle(title = "Settings", modifier = Modifier.padding(start = 2.dp))
        }

        // ── Appearance ─────────────────────────────────────────────
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Appearance",
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            EgSegmentedControl(
                options = modeOptions,
                selectedIndex = selectedMode,
                onSelect = { index ->
                    scope.launch { graph.theme.setMode(ThemeMode.entries[index]) }
                },
            )
            Text(
                text = ThemeMode.entries[selectedMode].hint,
                color = palette.muted,
                fontSize = 12.sp,
            )
        }

        // ── Notifications ──────────────────────────────────────────
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                Text(
                    text = "Notifications",
                    color = palette.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            EgPrimaryButton(
                label = "Send a test alert",
                tone = PillTone.Red,
                onClick = { scope.launch { graph.notifications.sendTestNotification() } },
            )
            DisclosureRow(
                title = "Manage in Android settings",
                subtitle = "Channels: Events · Stories · Actions",
            ) {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                }
                runCatching { context.startActivity(intent) }
            }
            Text(
                text = "New events & stories are picked up by a 6-hour background sync (WorkManager): battery and data friendly.",
                color = palette.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }

        // ── About ──────────────────────────────────────────────────
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = palette.eco, modifier = Modifier.size(20.dp))
                Text(
                    text = "About Earth Guardians",
                    color = palette.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            DisclosureRow(
                title = "earthguardians.org",
                subtitle = "Open the site in your browser",
            ) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.earthguardians.org")))
                }
            }
            DisclosureRow(
                title = "reachout@earthguardians.org",
                subtitle = "General contact",
            ) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:reachout@earthguardians.org")
                        },
                    )
                }
            }
            Text(
                text = "PO Box 1561 Laurel, MD 20725 · EIN 84-1397083",
                color = palette.muted,
                fontSize = 12.sp,
            )
        }

        Box(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.fillMaxWidth().height(22.dp))
            Text(
                text = "EG App v${BuildConfig.VERSION_NAME} — built youth-first on GitHub Actions.",
                color = palette.muted,
                fontSize = 12.sp,
            )
        }
    }
}
