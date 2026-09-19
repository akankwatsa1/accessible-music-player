package com.akankwatsa.accessiblemusicplayer.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akankwatsa.accessiblemusicplayer.MusicPlayerApp
import com.akankwatsa.accessiblemusicplayer.data.MediaTrack
import com.akankwatsa.accessiblemusicplayer.data.PlayerSettings
import com.akankwatsa.accessiblemusicplayer.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/** What the screen should show right now. */
sealed interface PlayerUiState {
    /** Waiting for the media permission decision. */
    data object NeedsPermission : PlayerUiState

    /** Scanning the device. */
    data object Loading : PlayerUiState

    /** The scan finished but found nothing. */
    data object Empty : PlayerUiState

    /** Tracks are available. */
    data object Ready : PlayerUiState
}

/** Which slice of the library the list shows. */
enum class LibraryFilter { ALL, FAVOURITES, VIDEOS, ALBUMS, ARTISTS }

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicPlayerApp
    private val library = app.libraryRepository
    private val settingsRepo = app.settingsRepository

    private val _allTracks = MutableStateFlow<List<MediaTrack>>(emptyList())
    val allTracks: StateFlow<List<MediaTrack>> = _allTracks.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(LibraryFilter.ALL)
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    /** Album and artist narrowing, applied on top of the filter mode. */
    private val _selectedAlbum = MutableStateFlow("")
    val selectedAlbum: StateFlow<String> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow("")
    val selectedArtist: StateFlow<String> = _selectedArtist.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var _lastScanAt = 0L

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    val settings: StateFlow<PlayerSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerSettings())

    val playback = PlaybackController.state

    /** The filtered list actually rendered by the screen. */
    val visibleTracks: StateFlow<List<MediaTrack>> = combine(
        _allTracks,
        _searchQuery,
        _filter,
        _selectedAlbum,
        _selectedArtist,
        settingsRepo.settings,
    ) { all, query, mode, album, artist, prefs ->
        filterTracks(all, query, mode, album, artist, prefs.favourites)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<PlayerUiState> =
        combine(_hasPermission, _scanning, _allTracks) { permission, scanning, tracks ->
            when {
                !permission -> PlayerUiState.NeedsPermission
                scanning && tracks.isEmpty() -> PlayerUiState.Loading
                tracks.isEmpty() -> PlayerUiState.Empty
                else -> PlayerUiState.Ready
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState.NeedsPermission)

    private fun filterTracks(
        tracks: List<MediaTrack>,
        query: String,
        filterMode: LibraryFilter,
        album: String,
        artist: String,
        favourites: Set<String>,
    ): List<MediaTrack> {
        val trimmed = query.trim()
        return tracks.filter { track ->
            val matchesFilter = when (filterMode) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVOURITES -> favourites.contains(track.stableKey)
                LibraryFilter.VIDEOS -> track.isVideo
                LibraryFilter.ALBUMS ->
                    album.isBlank() || track.album.equals(album, ignoreCase = true)
                LibraryFilter.ARTISTS ->
                    artist.isBlank() || track.artist.equals(artist, ignoreCase = true)
            }
            if (!matchesFilter) return@filter false
            if (trimmed.isEmpty()) return@filter true
            track.title.contains(trimmed, ignoreCase = true) ||
                track.artist.contains(trimmed, ignoreCase = true) ||
                track.album.contains(trimmed, ignoreCase = true)
        }
    }

    /** Distinct album names, for the album picker. */
    fun albumNames(): List<String> =
        _allTracks.value.map { it.album }.filter { it.isNotBlank() }.distinct().sorted()

    /** Distinct artist names, for the artist picker. */
    fun artistNames(): List<String> =
        _allTracks.value.map { it.artist }.filter { it.isNotBlank() }.distinct().sorted()

    fun onPermissionResult(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) rescan()
    }

    fun refreshPermissionState(granted: Boolean) {
        _hasPermission.value = granted
    }

    /** Rescans the device library. Safe to call repeatedly. */
    fun rescan(announce: Boolean = false) {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            val tracks = library.scanAll()
            _allTracks.value = tracks
            _lastScanAt = System.currentTimeMillis()
            _scanning.value = false
            if (announce) {
                _snackbar.value = "Library updated: " + tracks.size + " items"
            }
        }
    }

    /**
     * A MediaStore scan is fast but not free, so returning to the app only
     * rescans when the library is empty or the last scan is stale. Files added
     * while the app was away still show up, without a scan on every resume.
     */
    fun rescanIfStale() {
        if (_scanning.value) return
        if (_allTracks.value.isEmpty()) {
            rescan()
            return
        }
        val age = System.currentTimeMillis() - _lastScanAt
        if (age > STALE_SCAN_MS) rescan()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setFilter(filterMode: LibraryFilter) {
        _filter.value = filterMode
    }

    /** Picks the album to show. Passing a blank name shows every album. */
    fun selectAlbum(album: String) {
        _selectedAlbum.value = album
        _filter.value = LibraryFilter.ALBUMS
    }

    /** Picks the artist to show. Passing a blank name shows every artist. */
    fun selectArtist(artist: String) {
        _selectedArtist.value = artist
        _filter.value = LibraryFilter.ARTISTS
    }

    /**
     * Removes a track from the library list. The media file itself is left
     * alone: deleting a song from the device is a job for the gallery or file
     * manager, and this app never destroys the listener's files.
     */
    fun forgetTrack(track: MediaTrack) {
        val remaining = _allTracks.value.filterNot { it.stableKey == track.stableKey }
        _allTracks.value = remaining
        _snackbar.value = track.title + " removed from the list"
    }

    fun toggleFavourite(track: MediaTrack) {
        val wasFavourite = settings.value.favourites.contains(track.stableKey)
        viewModelScope.launch {
            settingsRepo.toggleFavourite(track.stableKey)
            _snackbar.value = if (wasFavourite) {
                track.title + " removed from favourites"
            } else {
                track.title + " added to favourites"
            }
        }
    }

    /** Starts playback of [track] inside the list it came from. */
    fun play(track: MediaTrack) {
        val list = visibleTracks.value
        val index = list.indexOfFirst { it.stableKey == track.stableKey }
        if (index >= 0) {
            PlaybackController.play(list, index)
        } else {
            PlaybackController.play(listOf(track), 0)
        }
    }

    /** Plays a queue of one, used when a file is opened from another app. */
    fun playExternal(uri: Uri) {
        viewModelScope.launch {
            val track = runCatching {
                withContext(Dispatchers.IO) { buildExternalTrack(uri) }
            }.getOrNull()
            if (track == null) {
                _snackbar.value = "That file could not be opened."
            } else {
                PlaybackController.play(listOf(track), 0)
            }
        }
    }

    private fun buildExternalTrack(uri: Uri): MediaTrack? {
        val resolver = getApplication<Application>().contentResolver
        val mime = resolver.getType(uri).orEmpty()
        var title = uri.lastPathSegment.orEmpty()
        var duration = 0L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val titleCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.TITLE)
                    val nameCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    val durCol = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DURATION)
                    if (titleCol >= 0) c.getString(titleCol)?.let { title = it }
                    if (title.isBlank() && nameCol >= 0) title = c.getString(nameCol).orEmpty()
                    if (durCol >= 0) duration = c.getLong(durCol)
                }
            }
        }
        if (title.isBlank()) return null
        val isVideo = mime.startsWith("video/")
        return MediaTrack(
            id = uri.hashCode().toLong(),
            uri = uri,
            title = title.substringBeforeLast('.'),
            artist = "",
            album = "",
            durationMs = duration,
            isVideo = isVideo,
            artworkUri = null,
            subtitle = if (isVideo) "Video" else "",
            relativePath = "",
        )
    }

    fun skipBack() {
        PlaybackController.skipBy(-SKIP_MS)
        _snackbar.value = "Skipped back 10 seconds"
    }

    fun skipForward() {
        PlaybackController.skipBy(SKIP_MS)
        _snackbar.value = "Skipped forward 10 seconds"
    }

    fun toggleShuffle() {
        val next = !playback.value.shuffle
        PlaybackController.setShuffle(next)
        viewModelScope.launch { settingsRepo.setShuffle(next) }
        _snackbar.value = if (next) "Shuffle on" else "Shuffle off"
    }

    fun cycleRepeat() {
        val next = when (playback.value.repeatMode) {
            PlayerSettings.REPEAT_OFF -> PlayerSettings.REPEAT_ALL
            PlayerSettings.REPEAT_ALL -> PlayerSettings.REPEAT_ONE
            else -> PlayerSettings.REPEAT_OFF
        }
        PlaybackController.setRepeatMode(next)
        viewModelScope.launch { settingsRepo.setRepeatMode(next) }
        _snackbar.value = when (next) {
            PlayerSettings.REPEAT_ALL -> "Repeat all"
            PlayerSettings.REPEAT_ONE -> "Repeat one"
            else -> "Repeat off"
        }
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 3f)
        PlaybackController.setSpeed(clamped)
        viewModelScope.launch { settingsRepo.setPlaybackSpeed(clamped) }
    }

    fun setVolume(volume: Float) {
        PlaybackController.setVolume(volume)
        viewModelScope.launch { settingsRepo.setVolume(volume) }
    }

    fun setDetailsExpanded(expanded: Boolean) {
        viewModelScope.launch { settingsRepo.setDetailsExpanded(expanded) }
    }

    fun setZoomFill(fill: Boolean) {
        viewModelScope.launch { settingsRepo.setZoomFill(fill) }
    }

    fun setSleepTimer(minutes: Int) {
        PlaybackController.setSleepTimer(minutes)
        _snackbar.value = if (minutes <= 0) {
            "Sleep timer cancelled"
        } else {
            "Sleep timer set for $minutes minutes"
        }
    }

    fun clearSnackbar() {
        _snackbar.value = null
    }

    fun showMessage(message: String) {
        _snackbar.value = message
    }

    /** Seeks to an absolute position, used by the position slider. */
    fun seekTo(positionMs: Long) {
        PlaybackController.seekTo(positionMs)
    }

    private companion object {
        const val SKIP_MS = 10_000L
        const val STALE_SCAN_MS = 30_000L
    }
}