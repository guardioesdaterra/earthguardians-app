package org.earthguardians.app

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import kotlinx.coroutines.launch
import org.earthguardians.app.graph.AppGraph
import org.earthguardians.app.notifications.SyncWorker

/**
 * Earth Guardians — official app.
 * Composition root; every screen reads the shared [AppGraph].
 */
class EGApp : Application(), Configuration.Provider {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.notifications.ensureChannels()
        SyncWorker.schedule(this)
        // Seed the seen-set so a cold sync doesn't spam everything at once.
        graph.scope.launch { graph.notifications.seedFromHistory() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    companion object {
        fun from(context: Context): EGApp = context.applicationContext as EGApp
    }
}
