package org.earthguardians.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.earthguardians.app.graph.Deeplinks
import org.earthguardians.app.graph.LocalAppGraph
import org.earthguardians.app.ui.root.EgAppRoot

/**
 * Single-activity, Compose-only app. External arrivals (notifications, deep
 * links, shortcuts, site https links) are funnelled into the RouteBus and
 * consumed by the navigation tree.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = EGApp.from(this).graph
        graph.routeBus.postRoute(Deeplinks.parseRoute(intent?.dataString ?: intent?.getStringExtra(EXTRA_ROUTE)))

        setContent {
            CompositionLocalProvider(LocalAppGraph provides graph) {
                EgAppRoot(graph)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        EGApp.from(this).graph.routeBus.postRoute(
            Deeplinks.parseRoute(intent.dataString ?: intent.getStringExtra(EXTRA_ROUTE)),
        )
    }

    companion object {
        const val EXTRA_ROUTE = "eg_route"
    }
}
