package org.earthguardians.app.ui.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.util.Log
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.earthguardians.app.data.model.EgEvent
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.model.SiteSection
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.graph.Deeplinks
import org.earthguardians.app.graph.LocalAppGraph
import org.earthguardians.app.ui.design.EgRadius
import org.earthguardians.app.ui.design.rememberTapHaptic
import org.earthguardians.app.ui.events.EventsScreen
import org.earthguardians.app.ui.events.EventDetailScreen
import org.earthguardians.app.ui.home.HomeScreen
import org.earthguardians.app.ui.maps.MapsScreen
import org.earthguardians.app.ui.more.MoreScreen
import org.earthguardians.app.ui.more.NotificationsScreen
import org.earthguardians.app.ui.more.SettingsScreen
import org.earthguardians.app.ui.stories.StoriesScreen
import org.earthguardians.app.ui.stories.StoryReaderScreen
import org.earthguardians.app.ui.theme.EGAppTheme
import org.earthguardians.app.ui.theme.ThemeMode
import org.earthguardians.app.ui.theme.egPalette
import org.earthguardians.app.ui.web.WebViewScreen

/** Canonical route names shared by screens and the bottom bar. */
object Routes {
    const val HOME = "home"
    const val EVENTS = "events"
    const val STORIES = "stories"
    const val MAPS = "maps"
    const val MORE = "more"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"

    const val EVENT_DETAIL = "event?e={e}"
    const val POST_DETAIL = "post?u={u}"
    const val SECTION = "section?u={u}"

    fun eventDetail(url: String) = "event?e=${Deeplinks.encode(url)}"
    fun postDetail(url: String) = "post?u=${Deeplinks.encode(url)}"
    fun section(url: String) = "section?u=${Deeplinks.encode(url)}"
}

internal data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    TopDestination(Routes.HOME, "Home", Icons.Rounded.Groups),
    TopDestination(Routes.EVENTS, "Events", Icons.Rounded.Event),
    TopDestination(Routes.STORIES, "Stories", Icons.Rounded.Eco),
    TopDestination(Routes.MAPS, "Maps", Icons.Rounded.Map),
    TopDestination(Routes.MORE, "More", Icons.Rounded.Menu),
)

/**
 * App shell: onboarding gate → tab scaffold with floating glass bottom bar.
 * External routes arrive via the RouteBus (notifications, links, shortcuts).
 */
@Composable
fun EgAppRoot(graph: AppGraph) {
    val mode by graph.theme.mode.collectAsState(initial = ThemeMode.System)
    EGAppTheme(mode) {
        val onboarded by graph.prefs.onboarded.collectAsState(initial = null)
        when (onboarded) {
            // Only until DataStore's first emission — a fresh install maps to
            // `false`, so this splash can never strand (see PrefsRepository).
            null -> Box(Modifier.fillMaxSize().background(egPalette.background))
            false -> OnboardingScreen(
                graph = graph,
                onFinished = { graph.scope.launch { graph.prefs.completeOnboarding() } },
            )
            else -> MainTabs(graph)
        }
    }
}

@Composable
private fun MainTabs(graph: AppGraph) {
    val navController = rememberNavController()
    val haptic = rememberTapHaptic()

    // Consume external arrivals (notification taps, site links, shortcuts).
    LaunchedEffect(navController) {
        graph.routeBus.pending.collectLatest { route ->
            if (route != null) {
                // A swallowed failure here once blanked the WebView screens:
                // navigate() throwing left the route unused with zero evidence.
                runCatching { navController.navigate(route) { launchSingleTop = true } }
                    .onFailure { Log.e("EgRoute", "navigate failed for '$route'", it) }
                graph.routeBus.consume()
            }
        }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val barVisible = currentRoute in bottomDestinations.map { it.route }

    Column(Modifier.fillMaxSize().background(egPalette.background)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().weight(1f),
            enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    graph = graph,
                    navigateToTab = { route -> navController.navigateInTab(route, haptic) },
                    openSettings = { navController.navigateSafely(Routes.SETTINGS) },
                    openNotifications = { navController.navigateSafely(Routes.NOTIFICATIONS) },
                    openEvent = { event -> navController.navigateSafely(Routes.eventDetail(event.url)) },
                    openStory = { post -> navController.navigateSafely(Routes.postDetail(post.url)) },
                    openSection = { section -> navController.navigateSafely(Routes.section(section.url)) },
                )
            }
            composable(Routes.EVENTS) {
                EventsScreen(
                    graph = graph,
                    openEvent = { event -> navController.navigateSafely(Routes.eventDetail(event.url)) },
                )
            }
            composable(Routes.STORIES) {
                StoriesScreen(
                    graph = graph,
                    openStory = { post -> navController.navigateSafely(Routes.postDetail(post.url)) },
                )
            }
            composable(Routes.MAPS) {
                MapsScreen(
                    graph = graph,
                    openUrl = { url -> navController.navigateSafely(Routes.section(url)) },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    graph = graph,
                    openSection = { section -> navController.navigateSafely(Routes.section(section.url)) },
                    openSettings = { navController.navigateSafely(Routes.SETTINGS) },
                    openNotifications = { navController.navigateSafely(Routes.NOTIFICATIONS) },
                    openUrl = { url -> navController.navigateSafely(Routes.section(url)) },
                )
            }

            // Navigation query arguments are already URI-decoded once; decoding
            // them again would corrupt escaped characters inside the page URL.
            composable(
                Routes.EVENT_DETAIL,
                arguments = listOf(navArgument("e") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                EventDetailScreen(
                    graph = graph,
                    eventUrl = entry.arguments?.getString("e").orEmpty(),
                    onBack = { popSafely(navController) },
                    onOpenPage = { url -> navController.navigateSafely(Routes.section(url)) },
                )
            }

            composable(
                Routes.POST_DETAIL,
                arguments = listOf(navArgument("u") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                StoryReaderScreen(
                    postUrl = entry.arguments?.getString("u").orEmpty(),
                    graph = graph,
                    onBack = { popSafely(navController) },
                    onOpenFull = { url -> navController.navigateSafely(Routes.section(url)) },
                )
            }

            composable(
                Routes.SECTION,
                arguments = listOf(navArgument("u") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                WebViewScreen(
                    url = entry.arguments?.getString("u").orEmpty(),
                    graph = graph,
                    onBack = { popSafely(navController) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(graph = graph, onBack = { popSafely(navController) })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(graph = graph, onBack = { popSafely(navController) })
            }
        }

        AnimatedVisibility(
            visible = barVisible,
            enter = slideInVertically(tween(220)) { it } + fadeIn(),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(),
        ) {
            EgBottomBar(navController, haptic)
        }
    }
}

private fun NavHostController.navigateInTab(route: String, haptic: () -> Unit) {
    haptic()
    navigate(route) {
        popUpTo(0) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateSafely(route: String) {
    runCatching { navigate(route) { launchSingleTop = true } }
}

private fun popSafely(navController: NavHostController) {
    if (!navController.popBackStack()) Unit
}

/** Floating glass bottom bar — the app's iOS-style tab switcher. */
@Composable
private fun EgBottomBar(navController: NavHostController, haptic: () -> Unit) {
    val palette = egPalette
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val shape = RoundedCornerShape(EgRadius.card)
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(palette.glass, shape)
            .border(1.dp, palette.glassStroke, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bottomDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            val tint by animateColorAsState(
                targetValue = if (selected) palette.accent else palette.muted,
                animationSpec = tween(220),
                label = "tabTint",
            )
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.14f else 1f,
                animationSpec = tween(220),
                label = "tabScale",
            )
            val interaction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(interactionSource = interaction, indication = null) {
                        haptic()
                        navController.navigate(destination.route) {
                            popUpTo(0) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = tint,
                    modifier = Modifier
                        .height(22.dp)
                        .scale(iconScale),
                )
                Text(
                    text = destination.label,
                    color = tint,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
                // iOS-style selection indicator: a small red pip under the active tab.
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(width = if (selected) 16.dp else 0.dp, height = 2.dp)
                        .clip(CircleShape)
                        .background(if (selected) palette.accent else Color.Transparent),
                )
            }
        }
    }
}
