package com.akankwatsa.accessiblemusicplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

/** Everything the player remembers between launches. */
data class PlayerSettings(
    val volume: Float = 1f,
    val shuffle: Boolean = false,
    val repeatMode: Int = REPEAT_OFF,
    val playbackSpeed: Float = 1f,
    val zoomFill: Boolean = false,
    val detailsExpanded: Boolean = true,
    val lastMediaKey: String = "",
    val lastPositionMs: Long = 0L,
    val favouritesOnly: Boolean = false,
    val favourites: Set<String> = emptySet(),
) {
    companion object {
        const val REPEAT_OFF = 0
        const val REPEAT_ALL = 1
        const val REPEAT_ONE = 2
    }
}

/**
 * Persists playback preferences and favourites with DataStore, which writes
 * off the main thread. This replaces the desktop app's settings.json.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<PlayerSettings> = context.dataStore.data.map { prefs ->
        PlayerSettings(
            volume = prefs[KEY_VOLUME] ?: 1f,
            shuffle = prefs[KEY_SHUFFLE] ?: false,
            repeatMode = prefs[KEY_REPEAT] ?: PlayerSettings.REPEAT_OFF,
            playbackSpeed = prefs[KEY_SPEED] ?: 1f,
            zoomFill = prefs[KEY_ZOOM_FILL] ?: false,
            detailsExpanded = prefs[KEY_DETAILS_EXPANDED] ?: true,
            lastMediaKey = prefs[KEY_LAST_MEDIA] ?: "",
            lastPositionMs = prefs[KEY_LAST_POSITION] ?: 0L,
            favouritesOnly = prefs[KEY_FAVOURITES_ONLY] ?: false,
            favourites = prefs[KEY_FAVOURITES] ?: emptySet(),
        )
    }

    suspend fun setVolume(value: Float) = edit { it[KEY_VOLUME] = value.coerceIn(0f, 1f) }
    suspend fun setShuffle(value: Boolean) = edit { it[KEY_SHUFFLE] = value }
    suspend fun setRepeatMode(value: Int) = edit { it[KEY_REPEAT] = value }
    suspend fun setPlaybackSpeed(value: Float) = edit { it[KEY_SPEED] = value }
    suspend fun setZoomFill(value: Boolean) = edit { it[KEY_ZOOM_FILL] = value }
    suspend fun setDetailsExpanded(value: Boolean) = edit { it[KEY_DETAILS_EXPANDED] = value }
    suspend fun setFavouritesOnly(value: Boolean) = edit { it[KEY_FAVOURITES_ONLY] = value }

    suspend fun rememberLastPlayed(mediaKey: String, positionMs: Long) = edit {
        it[KEY_LAST_MEDIA] = mediaKey
        it[KEY_LAST_POSITION] = positionMs
    }

    suspend fun toggleFavourite(mediaKey: String) = edit { prefs ->
        val current = prefs[KEY_FAVOURITES] ?: emptySet()
        prefs[KEY_FAVOURITES] = if (current.contains(mediaKey)) current - mediaKey else current + mediaKey
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val KEY_VOLUME = floatPreferencesKey("volume")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle")
        val KEY_REPEAT = intPreferencesKey("repeat_mode")
        val KEY_SPEED = floatPreferencesKey("playback_speed")
        val KEY_ZOOM_FILL = booleanPreferencesKey("zoom_fill")
        val KEY_DETAILS_EXPANDED = booleanPreferencesKey("details_expanded")
        val KEY_LAST_MEDIA = stringPreferencesKey("last_media_key")
        val KEY_LAST_POSITION = longPreferencesKey("last_position_ms")
        val KEY_FAVOURITES_ONLY = booleanPreferencesKey("favourites_only")
        val KEY_FAVOURITES = stringSetPreferencesKey("favourites")
    }
}