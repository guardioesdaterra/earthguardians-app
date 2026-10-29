package org.earthguardians.app.data.repo

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.prefsStore by preferencesDataStore(name = "eg_state")

@Serializable
data class StoredNotification(
    val id: String,
    val title: String,
    val message: String,
    val route: String,
    val postedAt: Long,
    val read: Boolean = false,
)

/**
 * App-level preferences: onboarding gate + the in-app notification center history.
 */
class PrefsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val store = context.prefsStore

    // ── Onboarding ────────────────────────────────────────────────────

    /** null = not yet loaded (splash), false = show onboarding, true = main app. */
    val onboarded: Flow<Boolean?> = store.data
        // Blanket fallback: a corrupt/unreadable store must never leave the
        // app on an empty splash screen — fresh state is recoverable.
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            when (prefs[KEY_ONBOARDED]) {
                true -> true
                false -> false
                null -> null
            }
        }

    suspend fun completeOnboarding() {
        store.edit { it[KEY_ONBOARDED] = true }
    }

    // ── Feed baseline (notification flood guard) ──────────────────────

    /**
     * URLs of feed items already accounted for by a sync. Without this, the
     * whole Squarespace back catalog reads as "new" after a fresh install and
     * every 6-hour sync re-drips old stories/events as notifications.
     */
    val feedSeen: Flow<Set<String>> = store.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[KEY_SEEN_FEED]
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
        }

    /** Persists a de-duplicated set (most-recent wins on duplicates). */
    suspend fun saveFeedSeen(urls: Collection<String>) {
        store.edit {
            it[KEY_SEEN_FEED] = urls.filter { url -> url.isNotBlank() }
                .distinct()
                .takeLast(400)
                .joinToString("\n")
        }
    }

    // ── Notification center ───────────────────────────────────────────

    val notifications: Flow<List<StoredNotification>> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> decodeHistory(prefs[KEY_HISTORY]) }

    suspend fun saveNotifications(list: List<StoredNotification>) {
        store.edit { it[KEY_HISTORY] = historyJson(list) }
    }

    private fun decodeHistory(raw: String?): List<StoredNotification> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(historyListSerializer, raw)
        }.getOrDefault(emptyList())
    }

    private fun historyJson(list: List<StoredNotification>): String =
        json.encodeToString(historyListSerializer, list)

    private companion object {
        val KEY_ONBOARDED = booleanPreferencesKey("onboarding_complete")
        val KEY_HISTORY = stringPreferencesKey("notification_history")
        val KEY_SEEN_FEED = stringPreferencesKey("feed_seen_urls")
        val historyListSerializer = ListSerializer(kotlinx.serialization.serializer<StoredNotification>())
    }
}
