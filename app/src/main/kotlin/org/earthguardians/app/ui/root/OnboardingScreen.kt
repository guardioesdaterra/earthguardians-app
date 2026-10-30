package org.earthguardians.app.ui.root

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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

private val onboardingGridImages = listOf(
    OfficialSiteImages.COMMUNITY,
    OfficialSiteImages.BENIN_ACTION,
    OfficialSiteImages.LEAVES,
    OfficialSiteImages.FIELDWORK,
    OfficialSiteImages.YOUTH_GATHERING,
    OfficialSiteImages.CLIMATE_ACTION,
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
                    OnboardingArtwork(page = page, slide = slide)
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

@Composable
private fun OnboardingBackdrop(page: Int) {
    val palette = LocalEGPalette.current
    Crossfade(
        targetState = page,
        animationSpec = tween(320),
        label = "onboardingBackdrop",
    ) { backgroundPage ->
        Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
            when (backgroundPage) {
                0 -> OnboardingPhotoGrid(modifier = Modifier.fillMaxSize(), opacity = 0.28f)
                1 -> Image(
                    painter = painterResource(OfficialSiteImages.BENIN_ACTION),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.65f },
                )
                else -> OfficialSitePhotoCollage(
                    page = backgroundPage,
                    modifier = Modifier.fillMaxSize(),
                    opacity = 0.10f,
                )
            }
            val topScrim = when (backgroundPage) {
                0 -> 0.55f
                1 -> 0.62f
                else -> 0.48f
            }
            val bottomScrim = if (backgroundPage == 1) 0.82f else topScrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.background.copy(alpha = topScrim),
                                palette.background.copy(alpha = bottomScrim),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingPhotoGrid(
    modifier: Modifier,
    opacity: Float,
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(5) { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) { column ->
                    val imageIndex = (row * 3 + column) % onboardingGridImages.size
                    Image(
                        painter = painterResource(onboardingGridImages[imageIndex]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .graphicsLayer {
                                alpha = opacity
                                rotationZ = if ((row + column) % 2 == 0) -1.5f else 1.5f
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingArtwork(
    page: Int,
    slide: OnboardingSlide,
) {
    val palette = LocalEGPalette.current
    if (page == 1) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(EgRadius.pill))
                .background(palette.glass.copy(alpha = 0.82f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(slide.icon, contentDescription = null, tint = palette.eco, modifier = Modifier.size(15.dp))
            Text("FULL-BLEED PHOTO · EARTH GUARDIANS", color = palette.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(EgRadius.card))
            .background(palette.glass)
            .semantics { contentDescription = slide.imageDescription },
    ) {
        when (page) {
            0 -> OnboardingGridArtwork()
            2 -> OnboardingOffsetArtwork(slide = slide)
            else -> {
                Image(
                    painter = painterResource(slide.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.52f)),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OnboardingGridArtwork() {
    Column(
        modifier = Modifier.fillMaxSize().padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(2) { column ->
                    val imageIndex = row * 2 + column
                    Image(
                        painter = painterResource(onboardingGridImages[imageIndex]),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .graphicsLayer { alpha = 0.76f },
                    )
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.52f)),
                ),
            ),
    )
    Text(
        text = "A GLOBAL COMMUNITY · 1 / ${slides.size}",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(13.dp)
            .clip(RoundedCornerShape(EgRadius.pill))
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun BoxScope.OnboardingOffsetArtwork(slide: OnboardingSlide) {
    Image(
        painter = painterResource(slide.imageRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 8.dp, bottom = 7.dp)
            .fillMaxWidth(0.72f)
            .height(154.dp)
            .clip(RoundedCornerShape(22.dp)),
    )
    Image(
        painter = painterResource(OfficialSiteImages.YOUTH_GATHERING),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 10.dp, end = 9.dp)
            .fillMaxWidth(0.48f)
            .height(106.dp)
            .clip(RoundedCornerShape(20.dp))
            .graphicsLayer { rotationZ = 3f },
    )
    Image(
        painter = painterResource(OfficialSiteImages.LEAVES),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 11.dp, bottom = 8.dp)
            .width(92.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .graphicsLayer { rotationZ = -4f },
    )
    Text(
        text = "FIELD NOTES",
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(14.dp)
            .clip(RoundedCornerShape(EgRadius.pill))
            .background(Color.Black.copy(alpha = 0.52f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Exposed for tests — what the four onboarding slides promise. */
internal val onboardingSlideCount: Int get() = slides.size
