package org.earthguardians.app.ui.root

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.common.OfficialSiteImages
import org.earthguardians.app.ui.common.OfficialSitePhotoCollage
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.LocalEGPalette

/** Four swipeable onboarding steps grounded in the official homepage. */
private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val feature: String,
    val imageRes: Int,
    val imageDescription: String,
)

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Rounded.Public,
        title = "Join a global\nclimate community",
        body = "Your network of climate leaders, created for youth by youth.",
        feature = "Youth-led across six continents",
        imageRes = OfficialSiteImages.COMMUNITY,
        imageDescription = "Young people and adults gathered outdoors beneath green trees.",
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Eco,
        title = "Turn local ideas\ninto action",
        body = "Explore crews, campaigns, events, and resources led by young people.",
        feature = "Find or start a crew",
        imageRes = OfficialSiteImages.BENIN_ACTION,
        imageDescription = "A youth climate leader speaking through a megaphone from a boat.",
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Map,
        title = "See the impact\nadd up",
        body = "Discover the stories, partnerships, and projects behind a healthier future.",
        feature = "Local action · lasting impact",
        imageRes = OfficialSiteImages.FIELDWORK,
        imageDescription = "Earth Guardians members forming a circle on a grassy field, seen from above.",
    ),
    OnboardingSlide(
        icon = Icons.Rounded.NotificationsActive,
        title = "Stay close to\nwhat’s next",
        body = "Follow new crew events and stories, and choose whether alerts are right for you.",
        feature = "Choose alerts anytime",
        imageRes = OfficialSiteImages.CLIMATE_ACTION,
        imageDescription = "Young climate activists marching together beneath trees.",
    ),
)

@Composable
internal fun OnboardingScreen(
    graph: AppGraph,
    onFinished: () -> Unit,
) {
    val palette = LocalEGPalette.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isLastPage by remember {
        derivedStateOf { pagerState.currentPage == slides.lastIndex }
    }
    var notificationsWanted by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    fun requestNotificationsIfWanted() {
        if (!notificationsWanted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            graph.appContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        OfficialSitePhotoCollage(
            page = pagerState.currentPage,
            modifier = Modifier.fillMaxSize(),
            opacity = 0.075f,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.eco.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.08f),
                            radius = size.width * 0.72f,
                        ),
                        radius = size.width * 0.72f,
                        center = Offset(size.width * 0.92f, size.height * 0.08f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.eco.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.08f, size.height * 0.98f),
                            radius = size.width * 0.78f,
                        ),
                        radius = size.width * 0.78f,
                        center = Offset(size.width * 0.08f, size.height * 0.98f),
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(184.dp)
                            .clip(RoundedCornerShape(EgRadius.card)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(palette.eco.copy(alpha = 0.42f), palette.glass))),
                        )
                        Image(
                            painter = painterResource(slide.imageRes),
                            contentDescription = slide.imageDescription,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = 0.42f },
                        )
                        OfficialSitePhotoCollage(page = page, modifier = Modifier.fillMaxSize(), opacity = 0.13f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.66f)),
                                    ),
                                ),
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(EgRadius.pill))
                                .background(Color.Black.copy(alpha = 0.42f))
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(slide.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text(
                                text = "${page + 1} / ${slides.size} · EARTH GUARDIANS",
                                color = Color.White,
                                fontSize = 10.sp,
                                letterSpacing = 0.7.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "Photo · earthguardians.org",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                        )
                    }

                    Text(
                        text = slide.title,
                        color = palette.onBackground,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.7).sp,
                    )
                    Text(
                        text = slide.body,
                        color = palette.muted,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(EgRadius.pill))
                            .background(palette.eco.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(slide.icon, contentDescription = null, tint = palette.eco, modifier = Modifier.size(16.dp))
                        Text(slide.feature, color = palette.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (page == slides.lastIndex) {
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
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
                            Text("Event & story alerts", color = palette.onBackground, fontSize = 15.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                repeat(slides.size) { index ->
                    val active = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (active) 22.dp else 6.dp,
                        animationSpec = tween(220),
                        label = "onboardingDotWidth",
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (active) palette.eco else palette.outline,
                        animationSpec = tween(220),
                        label = "onboardingDotColor",
                    )
                    Box(
                        modifier = Modifier
                            .size(width = dotWidth, height = 6.dp)
                            .clip(CircleShape)
                            .background(dotColor, CircleShape),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isLastPage) {
                    Spacer(Modifier.width(48.dp))
                } else {
                    Text(
                        text = "Skip",
                        color = palette.muted,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(EgRadius.pill))
                            .clickable { onFinished() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                EgPrimaryButton(
                    label = if (isLastPage) "Get started" else if (pagerState.currentPage == 0) "Explore" else "Next",
                    tone = PillTone.Green,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isLastPage) {
                            requestNotificationsIfWanted()
                            onFinished()
                        } else {
                            scope.launch {
                                runCatching { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Exposed for tests — what the four onboarding slides promise. */
internal val onboardingSlideCount: Int get() = slides.size
