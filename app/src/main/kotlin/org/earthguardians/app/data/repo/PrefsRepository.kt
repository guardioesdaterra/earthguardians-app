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
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
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
        val historyListSerializer = ListSerializer(kotlinx.serialization.serializer<StoredNotification>())
    }
}
