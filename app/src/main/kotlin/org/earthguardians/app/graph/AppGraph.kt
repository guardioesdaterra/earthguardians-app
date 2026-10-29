package org.earthguardians.app.graph

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.earthguardians.app.data.repo.ContentRepository
import org.earthguardians.app.data.repo.PrefsRepository
import org.earthguardians.app.notifications.NotificationCoordinator
import org.earthguardians.app.ui.theme.ThemeRepository

/**
 * Pure-Kotlin deep link routing shared by MainActivity and unit tests.
 * Supported links:
 *  - earthguardians://home | events | stories | maps | more
 *  - earthguardians://event?e=...   earthguardians://post?u=...   earthguardians://section?u=...
 *  - https://www.earthguardians.org/<path> (any page of the site → in-app themed WebView)
 */
object Deeplinks {

    val NAV_TABS = setOf("home", "events", "stories", "maps", "more")

    fun encode(value: String): String = java.net.URLEncoder.encode(value, "UTF-8")

    fun decode(value: String?): String =
        value?.let { java.net.URLDecoder.decode(it, "UTF-8") }.orEmpty()

    /** Main-thread entry: MainActivity hands over `intent.dataString`. */
    fun parseRoute(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val uri = uriString.trim()

        // Custom scheme: earthguardians://<target>[?args]
        if (uri.startsWith("earthguardians://")) {
            val rest = uri.removePrefix("earthguardians://").trimStart('/')
            val noQuery = rest.substringBefore('?')
            val query = rest.substringAfter('?', "")
            val path = noQuery.substringBefore('/')
            return when {
                path in NAV_TABS -> path
                path == "event" && query.isNotBlank() -> "event?e=${query.encodeComponent("e")}"
                path == "post" && query.isNotBlank() -> "post?u=${query.encodeComponent("u")}"
                path == "section" && query.isNotBlank() -> "section?u=${query.encodeComponent("u")}"
                else -> null
            }
        }

        // Web app links: every page of the site opens in the themed in-app browser.
        if (uri.startsWith("http://", ignoreCase = true) || uri.startsWith("https://", ignoreCase = true)) {
            val host = runCatching { java.net.URI(uri).host?.lowercase() }.getOrNull()
            if (host == "www.earthguardians.org" || host == "earthguardians.org") {
                return "section?u=${encode(uri)}"
            }
        }
        return null
    }

    private fun String.encodeComponent(key: String): String {
        val value = substringAfter("$key=").substringBefore('&')
        // Incoming values may already be percent-encoded (e.g. app shortcuts built
        // with encode()); decode first so we never double-encode, then normalize.
        // For raw (unencoded) values this is a no-op round trip.
        val decoded = runCatching { java.net.URLDecoder.decode(value, "UTF-8") }
            .getOrDefault(value)
        return encode(decoded)
    }
}

/** Main-thread record of routes arriving from outside the Compose tree. */
class RouteBus {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun postRoute(route: String?) {
        if (route != null) _pending.value = route
    }

    fun consume() {
        _pending.value = null
    }
}

/**
 * Composition root: app-scoped singletons, wired once and shared across the
 * composition and configuration changes (no DI framework needed at this size).
 */
class AppGraph(context: Context) {
    val appContext: Context = context.applicationContext
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val theme = ThemeRepository(appContext)
    val prefs = PrefsRepository(appContext)
    val content = ContentRepository(appContext, scope)
    val notifications = NotificationCoordinator(appContext, prefs)
    val routeBus = RouteBus()
}

/** Shared handle for every screen/UI component of the app. */
val LocalAppGraph = staticCompositionLocalOf<AppGraph> {
    error("AppGraph is not provided — compose inside MainActivity")
}
