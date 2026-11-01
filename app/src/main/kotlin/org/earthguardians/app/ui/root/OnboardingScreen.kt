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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import org.earthguardians.app.ui.design.egPressScale
import org.earthguardians.app.ui.design.rememberPressInteraction
import org.earthguardians.app.ui.design.rememberTapHaptic
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

// Six owner-approved homepage photos, in the documented catalog order:
// community, benin action, leaves, fieldwork, youth gathering, climate action.
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
    // Slide 2's filmstrip live-edits the full-bleed backdrop; default photo = BENIN_ACTION.
    var filmstripImageIndex by remember { mutableIntStateOf(1) }
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
        OnboardingBackdrop(page = currentPage, filmstripImageIndex = filmstripImageIndex)
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
                    OnboardingShowcase(
                        page = page,
                        slide = slide,
                        filmstripImageIndex = filmstripImageIndex,
                        onFilmstripSelect = { filmstripImageIndex = it },
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

/** Per-page backdrop treatments: wallpaper grid, live full-bleed photo, calm pillar wall. */
@Composable
private fun OnboardingBackdrop(
    page: Int,
    filmstripImageIndex: Int,
) {
    val palette = LocalEGPalette.current
    Crossfade(
        targetState = page,
        animationSpec = tween(320),
        label = "onboardingBackdrop",
    ) { backgroundPage ->
        Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
            when (backgroundPage) {
                0 -> OnboardingPhotoGrid(modifier = Modifier.fillMaxSize(), opacity = 0.28f)
                1 -> OnboardingFullBleed(
                    photo = onboardingGridImages[filmstripImageIndex],
                    opacity = 0.65f,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> OnboardingPillarWall(page = backgroundPage, opacity = 0.10f)
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
                            .graphicsLayer { alpha = opacity },
                    )
                }
            }
        }
    }
}

/** Pillar-wall photo sets for the calm backdrop pages, in catalog indexes. */
private fun onboardingPillarIndexes(page: Int): List<Int> = when (page) {
    2 -> listOf(3, 4, 2)
    3 -> listOf(5, 1, 0)
    else -> listOf(0, 1, 3)
}

/** Side-by-side vertical photo pillars — a wallpaper with zero overlapping photos. */
@Composable
private fun OnboardingPillarWall(
    page: Int,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        onboardingPillarIndexes(page).forEachIndexed { index, photo ->
            Image(
                painter = painterResource(onboardingGridImages[photo]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(if (index == 1) 1.15f else 1f)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = opacity },
            )
        }
    }
}

/** The filmstrip-controlled full-screen photo backdrop for the action slide. */
@Composable
private fun OnboardingFullBleed(
    photo: Int,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = photo,
        animationSpec = tween(320),
        label = "onboardingFullBleed",
        modifier = modifier,
    ) { current ->
        Image(
            painter = painterResource(current),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = opacity },
        )
    }
}

/** Each slide gets its own image-showcase component type — never a repeated rectangle. */
@Composable
private fun OnboardingShowcase(
    page: Int,
    slide: OnboardingSlide,
    filmstripImageIndex: Int,
    onFilmstripSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (page) {
        0 -> OnboardingMosaicShowcase(slide = slide, modifier = modifier)
        1 -> OnboardingFilmstripShowcase(
            slide = slide,
            selected = filmstripImageIndex,
            onSelect = onFilmstripSelect,
            modifier = modifier,
        )
        2 -> OnboardingFeatureDeckShowcase(slide = slide, modifier = modifier)
        else -> OnboardingSpotlightShowcase(slide = slide, modifier = modifier)
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

/** Slide 1 — a bento mosaic of four photos in different shapes; tap any tile to cycle it. */
@Composable
private fun OnboardingMosaicShowcase(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier,
) {
    val tapHaptic = rememberTapHaptic()
    val mosaicSlots = remember { mutableStateListOf(0, 3, 4, 1) }

    Column(
        modifier = modifier.semantics { contentDescription = slide.imageDescription },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OnboardingCaptionChip(text = "A GLOBAL COMMUNITY · TAP ANY PHOTO")
        Row(
            modifier = Modifier.fillMaxWidth().height(114.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingTile(
                res = onboardingGridImages[mosaicSlots[0]],
                cornerRadius = 24.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    mosaicSlots[0] = (mosaicSlots[0] + 1) % onboardingGridImages.size
                    tapHaptic()
                },
            )
            OnboardingTile(
                res = onboardingGridImages[mosaicSlots[1]],
                cornerRadius = 18.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    mosaicSlots[1] = (mosaicSlots[1] + 1) % onboardingGridImages.size
                    tapHaptic()
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingTile(
                res = onboardingGridImages[mosaicSlots[2]],
                cornerRadius = 29.dp,
                modifier = Modifier.size(58.dp),
                onClick = {
                    mosaicSlots[2] = (mosaicSlots[2] + 1) % onboardingGridImages.size
                    tapHaptic()
                },
            )
            OnboardingTile(
                res = onboardingGridImages[mosaicSlots[3]],
                cornerRadius = 29.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    mosaicSlots[3] = (mosaicSlots[3] + 1) % onboardingGridImages.size
                    tapHaptic()
                },
            )
        }
    }
}

/** Slide 2 — a filmstrip selector that live-swaps the full-bleed photo behind the copy. */
@Composable
private fun OnboardingFilmstripShowcase(
    slide: OnboardingSlide,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalEGPalette.current
    val tapHaptic = rememberTapHaptic()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OnboardingCaptionChip(
            text = "FULL-BLEED PHOTO · ${selected + 1} / ${onboardingGridImages.size}",
            pulse = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            onboardingGridImages.forEachIndexed { index, photo ->
                val isSelected = index == selected
                val width by animateDpAsState(
                    targetValue = if (isSelected) 72.dp else 44.dp,
                    animationSpec = tween(220),
                    label = "onboardingStripWidth$index",
                )
                val height by animateDpAsState(
                    targetValue = if (isSelected) 56.dp else 44.dp,
                    animationSpec = tween(220),
                    label = "onboardingStripHeight$index",
                )
                val corner by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 22.dp,
                    animationSpec = tween(220),
                    label = "onboardingStripCorner$index",
                )
                Box(
                    modifier = Modifier
                        .size(width = width, height = height)
                        .semantics {
                            contentDescription = "Photo ${index + 1} of ${onboardingGridImages.size}"
                            selected = isSelected
                            role = Role.Button
                        },
                ) {
                    OnboardingTile(
                        res = photo,
                        cornerRadius = corner,
                        borderWidth = if (isSelected) 2.dp else 1.dp,
                        borderColor = if (isSelected) palette.eco else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            onSelect(index)
                            tapHaptic()
                        },
                    )
                }
            }
        }
    }
}

/** Slide 3 — a featured photo viewer plus three circular thumbnails to swap it with. */
@Composable
private fun OnboardingFeatureDeckShowcase(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier,
) {
    val palette = LocalEGPalette.current
    val tapHaptic = rememberTapHaptic()
    val deckPhotos = listOf(slide.imageRes, OfficialSiteImages.YOUTH_GATHERING, OfficialSiteImages.COMMUNITY)
    var deckIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.semantics { contentDescription = slide.imageDescription },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OnboardingCaptionChip(text = "FIELD NOTES · LOCAL ACTION")
        OnboardingTile(
            res = deckPhotos[deckIndex],
            cornerRadius = 26.dp,
            gradientOverlay = true,
            modifier = Modifier.fillMaxWidth().height(128.dp),
            onClick = {
                deckIndex = (deckIndex + 1) % deckPhotos.size
                tapHaptic()
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            deckPhotos.forEachIndexed { index, photo ->
                val isSelected = index == deckIndex
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .semantics {
                            contentDescription = "Show photo ${index + 1} of ${deckPhotos.size}"
                            selected = isSelected
                            role = Role.Button
                        },
                ) {
                    OnboardingTile(
                        res = photo,
                        cornerRadius = 22.dp,
                        borderWidth = if (isSelected) 2.dp else 1.dp,
                        borderColor = if (isSelected) palette.eco else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            deckIndex = index
                            tapHaptic()
                        },
                    )
                }
            }
        }
    }
}

/** Slide 4 — a wide cinematic banner with a square accent and a full-pill photo beside it. */
@Composable
private fun OnboardingSpotlightShowcase(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier,
) {
    val tapHaptic = rememberTapHaptic()
    var spotlightBanner by remember { mutableIntStateOf(5) }
    var spotlightSquare by remember { mutableIntStateOf(2) }
    var spotlightPill by remember { mutableIntStateOf(4) }

    Column(
        modifier = modifier.semantics { contentDescription = slide.imageDescription },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OnboardingCaptionChip(text = "STAY IN THE LOOP", pulse = true)
        OnboardingTile(
            res = onboardingGridImages[spotlightBanner],
            cornerRadius = 28.dp,
            gradientOverlay = true,
            modifier = Modifier.fillMaxWidth().height(140.dp),
            onClick = {
                spotlightBanner = (spotlightBanner + 1) % onboardingGridImages.size
                tapHaptic()
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OnboardingTile(
                res = onboardingGridImages[spotlightSquare],
                cornerRadius = 14.dp,
                modifier = Modifier.size(56.dp),
                onClick = {
                    spotlightSquare = (spotlightSquare + 1) % onboardingGridImages.size
                    tapHaptic()
                },
            )
            OnboardingTile(
                res = onboardingGridImages[spotlightPill],
                cornerRadius = 28.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    spotlightPill = (spotlightPill + 1) % onboardingGridImages.size
                    tapHaptic()
                },
            )
        }
    }
}

/**
 * One pressable, crossfading photo tile with iOS-style hairline and springy press scale.
 * Every foreground photo in onboarding renders through this tile — never stacked, never rotated.
 */
@Composable
private fun OnboardingTile(
    res: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = EgRadius.tile,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.35f),
    gradientOverlay: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val interaction = rememberPressInteraction()
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(
                when {
                    onClick != null ->
                        Modifier.clickable(interactionSource = interaction, indication = null) {
                            onClick()
                        }
                    else -> Modifier
                },
            )
            .egPressScale(interaction),
    ) {
        Crossfade(
            targetState = res,
            animationSpec = tween(300),
            label = "onboardingTilePhoto",
        ) { current ->
            Image(
                painter = painterResource(current),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
