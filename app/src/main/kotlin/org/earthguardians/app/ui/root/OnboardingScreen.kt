package org.earthguardians.app.ui.root

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.ui.common.OfficialSiteImages
import org.earthguardians.app.ui.design.EgPrimaryButton
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.PillTone
import org.earthguardians.app.ui.theme.LocalEGPalette

/**
 * Four swipeable onboarding steps grounded in the official homepage.
 * Every slide renders exactly two bundled photos: one full-bleed backdrop and
 * one showcase tile — never a grid, mosaic or stack of overlapping images.
 */
private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val feature: String,
    val imageRes: Int,
    val imageDescription: String,
    val backdropRes: Int,
    val caption: String,
    val captionPulse: Boolean = false,
)

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Rounded.Public,
        title = "Join a global\nclimate community",
        body = "Your network of climate leaders, created for youth by youth.",
        feature = "Youth-led across six continents",
        imageRes = OfficialSiteImages.COMMUNITY,
        imageDescription = "Young people and adults gathered outdoors beneath green trees.",
        backdropRes = OfficialSiteImages.LEAVES,
        caption = "A GLOBAL COMMUNITY",
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Eco,
        title = "Turn local ideas\ninto action",
        body = "Explore crews, campaigns, events, and resources led by young people.",
        feature = "Find or start a crew",
        imageRes = OfficialSiteImages.BENIN_ACTION,
        imageDescription = "A youth climate leader speaking through a megaphone from a boat.",
        backdropRes = OfficialSiteImages.YOUTH_GATHERING,
        caption = "YOUTH IN ACTION",
    ),
    OnboardingSlide(
        icon = Icons.Rounded.Map,
        title = "See the impact\nadd up",
        body = "Discover the stories, partnerships, and projects behind a healthier future.",
        feature = "Local action · lasting impact",
        imageRes = OfficialSiteImages.FIELDWORK,
        imageDescription = "Earth Guardians members forming a circle on a grassy field, seen from above.",
        backdropRes = OfficialSiteImages.COMMUNITY,
        caption = "FIELD NOTES · LOCAL ACTION",
    ),
    OnboardingSlide(
        icon = Icons.Rounded.NotificationsActive,
        title = "Stay close to\nwhat’s next",
        body = "Follow new crew events and stories, and choose whether alerts are right for you.",
        feature = "Choose alerts anytime",
        imageRes = OfficialSiteImages.CLIMATE_ACTION,
        imageDescription = "Young climate activists marching together beneath trees.",
        backdropRes = OfficialSiteImages.FIELDWORK,
        caption = "STAY IN THE LOOP",
        captionPulse = true,
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
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val isLastPage by remember { derivedStateOf { currentPage == slides.lastIndex } }
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
        OnboardingBackdrop(page = currentPage)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    OnboardingPhotoShowcase(
                        slide = slide,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slides.forEachIndexed { index, _ ->
                    val active = currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (active) 22.dp else 6.dp,
                        animationSpec = tween(220),
                        label = "onboardingDotWidth$index",
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (active) palette.eco else palette.outline,
                        animationSpec = tween(220),
                        label = "onboardingDotColor$index",
                    )
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .semantics {
                                contentDescription = "Page ${index + 1} of ${slides.size}"
                                selected = active
                                role = Role.Tab
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = dotWidth, height = 6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFinished()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                EgPrimaryButton(
                    label = if (isLastPage) "Get started" else if (currentPage == 0) "Explore" else "Next",
                    tone = PillTone.Green,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isLastPage) {
                            requestNotificationsIfWanted()
                            onFinished()
                        } else {
                            scope.launch {
                                runCatching { pagerState.animateScrollToPage(currentPage + 1) }
                            }
                        }
                    },
                )
            }
        }
    }
}

/**
 * Per-page backdrop: a single full-bleed official photo dimmed by one vertical
 * scrim. No wallpaper grids, no pillar walls — exactly one image behind the copy.
 */
@Composable
private fun OnboardingBackdrop(
    page: Int,
) {
    val palette = LocalEGPalette.current
    Crossfade(
        targetState = page,
        animationSpec = tween(320),
        label = "onboardingBackdrop",
    ) { backgroundPage ->
        Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
            Image(
                painter = painterResource(slides[backgroundPage].backdropRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.65f },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.background.copy(alpha = 0.62f),
                                palette.background.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )
        }
    }
}

/** One photo per slide: a caption chip above a single full-width showcase tile. */
@Composable
private fun OnboardingPhotoShowcase(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics { contentDescription = slide.imageDescription },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OnboardingCaptionChip(text = slide.caption, pulse = slide.captionPulse)
        OnboardingTile(
            res = slide.imageRes,
            cornerRadius = 26.dp,
            gradientOverlay = true,
            modifier = Modifier.fillMaxWidth().height(140.dp),
        )
    }
}

/** Small glass status chip above every showcase; pulse = the view is live/interactive. */
@Composable
private fun OnboardingCaptionChip(
    text: String,
    pulse: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalEGPalette.current
    val transition = rememberInfiniteTransition(label = "onboardingChipPulse")
    val dotAlpha by transition.animateFloat(
        initialValue = if (pulse) 0.25f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(820), RepeatMode.Reverse),
        label = "onboardingChipDotAlpha",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(EgRadius.pill))
            .background(palette.glass.copy(alpha = 0.82f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(palette.eco)
                .graphicsLayer { alpha = dotAlpha },
        )
        Text(
            text = text,
            color = palette.onBackground,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
    }
}

/**
 * One decorative photo tile with an iOS-style hairline.
 * Every foreground photo in onboarding renders through this tile — exactly one
 * image per slide, never stacked, never rotated, never at reduced opacity.
 */
@Composable
private fun OnboardingTile(
    res: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = EgRadius.tile,
    gradientOverlay: Boolean = false,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape),
    ) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (gradientOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.40f),
                            ),
                        ),
                    ),
            )
        }
    }
}

/** Exposed for tests — what the four onboarding slides promise. */
internal val onboardingSlideCount: Int get() = slides.size
