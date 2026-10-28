package org.earthguardians.app.ui.root

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.LocalEGPalette
import org.earthguardians.app.ui.theme.egPalette

/** The four full-screen onboarding cards (Apple-style pitch, brand palette only). */
private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val tone: SlideTone,
)

private enum class SlideTone { Red, Green, Neutral }

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Rounded.Public,
        title = "Join a global\nclimate community",
        body = "Earth Guardians is a revolutionary environmental nonprofit run for youth, by youth — education, micro-grants, trainings and crews on six continents.",
        tone = SlideTone.Red,
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Eco,
        title = "Never lose the thread",
        body = "Fresh stories from the Earth Guardian blog and every crew event, streamed in offline-first and ready right on your fingertips.",
        tone = SlideTone.Green,
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Map,
        title = "See your world whole",
        body = "EG-Maps folds crews, campaigns and the endangered species atlas into one living map inside the app.",
        tone = SlideTone.Green,
    ),
    OnboardingSlide(
        icon = Icons.Rounded.NotificationsActive,
        title = "It starts with one tap",
        body = "Allow alerts for breaking events, new stories and urgent actions — you can retune it any time in Settings.",
        tone = SlideTone.Red,
    ),
)

@Composable
internal fun OnboardingScreen(
    graph: AppGraph,
    onFinished: () -> Unit,
) {
    val palette = LocalEGPalette.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isLastPage by remember {
        derivedStateOf { pagerState.currentPage == slides.lastIndex }
    }
    var notificationsWanted by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Work schedule already registered; permission decides whether pushes show.
        Unit
    }

    fun requestNotificationsIfWanted() {
        if (!notificationsWanted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                graph.appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .drawBehind {
                // soft brand glows (red top-right, green bottom-left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.9f, size.height * 0.1f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.eco.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.95f),
                        radius = size.width * 0.75f,
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.1f, size.height * 0.95f),
                )
            }
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "EARTH GUARDIANS",
                color = palette.onBackground,
                fontSize = 13.sp,
                letterSpacing = 2.4.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val slide = slides[page]
            val accentColor = when (slide.tone) {
                SlideTone.Red -> palette.accent
                SlideTone.Green -> palette.eco
                SlideTone.Neutral -> palette.onBackground
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Text(
                    text = slide.title,
                    color = palette.onBackground,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    text = slide.body,
                    color = palette.muted,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                )
                if (page == slides.lastIndex) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Switch(
                            checked = notificationsWanted,
                            onCheckedChange = { notificationsWanted = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.onEco,
                                checkedTrackColor = palette.eco,
                                uncheckedThumbColor = palette.background,
                                uncheckedTrackColor = palette.outline,
                            ),
                        )
                        Text(
                            "Event & story alerts",
                            color = palette.onBackground,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }

        // Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            repeat(slides.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> palette.accent
                                else -> palette.outline
                            },
                            CircleShape,
                        )
                        .size(width = if (active) 22.dp else 6.dp, height = 6.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isLastPage) "" else "Skip",
                color = palette.muted,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(EgRadius.pill))
                    .clickable { onFinished() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            EgPrimaryButton(
                label = when {
                    isLastPage -> "Get started"
                    pagerState.currentPage == 0 -> "Explore"
                    else -> "Next"
                },
                tone = if (isLastPage) PillTone.Green else PillTone.Red,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isLastPage) {
                        requestNotificationsIfWanted()
                        onFinished()
                    } else {
                        scope.launch {
                            runCatching {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Exposed for tests — what the 4 slides promise. */
internal val onboardingSlideCount: Int get() = slides.size
