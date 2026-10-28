package org.earthguardians.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.earthguardians.app.MainActivity
import org.earthguardians.app.R
import org.earthguardians.app.data.model.EgFeed
import org.earthguardians.app.data.model.EgPost
import org.earthguardians.app.data.repo.StoredNotification
import java.util.concurrent.atomic.AtomicInteger

/**
 * All system-notification plumbing: channels, permission state, push logic and
 * the in-app Notification Center history (Apple-style; mirrors what was posted).
 */
class NotificationCoordinator(
    private val context: Context,
    private val prefs: org.earthguardians.app.data.repo.PrefsRepository,
) {

    private val notifications = NotificationManagerCompat.from(context)
    private val notificationCounter = AtomicInteger(100)
    private val seenIds = mutableSetOf<String>()

    enum class Channel(val id: String, val titleRes: Int, val descRes: Int) {
        EVENTS("eg_channel_events", R.string.channel_events, R.string.channel_events_desc),
        STORIES("eg_channel_stories", R.string.channel_stories, R.string.channel_stories_desc),
        ACTIONS("eg_channel_actions", R.string.channel_actions, R.string.channel_actions_desc),
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        Channel.entries.forEach { channel ->
            val ch = NotificationChannel(
                channel.id,
                context.getString(channel.titleRes),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(channel.descRes)
                enableVibration(true)
                lightColor = 0xFFFF3B30.toInt() // brand red
            }
            manager.createNotificationChannel(ch)
        }
    }

    fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notifications.areNotificationsEnabled()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

    /** Marks the seen set from persisted history so a sync never double-notifies. */
    suspend fun seedFromHistory() {
        prefs.notifications.first().forEach { seenIds += it.id }
    }

    /** Called by SyncWorker after a successful feed refresh. */
    suspend fun onFeedSynced(feed: EgFeed) {
        seedFromHistory()
        val known = seenIds.toSet()

        val newEvents = feed.events.filter { it.url !in known }.take(2)
        val newStories = feed.posts.filter { it.url !in known }.take(3)

        newEvents.forEach { post ->
            push(
                title = context.getString(org.earthguardians.app.R.string.notif_event_title),
                message = post.title,
                route = routeForEvent(post),
                channel = Channel.EVENTS,
            )
        }
        newStories.forEach { story ->
            push(
                title = context.getString(org.earthguardians.app.R.string.notif_story_title),
                message = story.title,
                route = routeForPost(story),
                channel = Channel.STORIES,
            )
        }
    }

    fun sendTestNotification() {
        push(
            title = context.getString(org.earthguardians.app.R.string.notif_test_title),
            message = context.getString(org.earthguardians.app.R.string.notif_test_message),
            route = "home",
            channel = Channel.ACTIONS,
        )
    }

    private fun routeForPost(post: EgPost): String =
        "post?u=${Uri.encode(post.url)}"

    private fun routeForEvent(post: org.earthguardians.app.data.model.EgEvent): String =
        "event?e=${Uri.encode(post.url)}"

    suspend fun push(title: String, message: String, route: String, channel: Channel) {
        val deepLink = "earthguardians://$route"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            setPackage(context.packageName)
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationCounter.incrementAndGet(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val record = StoredNotification(
            id = route,
            title = title,
            message = message,
            route = route,
            postedAt = System.currentTimeMillis(),
        )

        val builder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_stat_eg)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(ContextCompat.getColor(context, org.earthguardians.app.R.color.eg_red))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // POST_NOTIFICATIONS is a runtime permission; silent no-op when not granted.
        runCatching { notifications.notify(record.id.hashCode(), builder.build()) }

        // Mirror into the in-app Notification Center.
        val existing = prefs.notifications.first()
        prefs.saveNotifications(listOf(record) + existing.take(29))
    }

    val history: Flow<List<StoredNotification>> = prefs.notifications
}
