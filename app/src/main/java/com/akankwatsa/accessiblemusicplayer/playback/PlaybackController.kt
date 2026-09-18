package com.akankwatsa.accessiblemusicplayer.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.akankwatsa.accessiblemusicplayer.data.MediaTrack
import com.akankwatsa.accessiblemusicplayer.data.PlayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A snapshot of the player, rendered by the Compose interface. */
data class PlaybackState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val mediaKey: String = "",
    val title: String = "",
    val subtitle: String = "",
    val isVideo: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = PlayerSettings.REPEAT_OFF,
    val speed: Float = 1f,
)

/**
 * Single shared bridge between the playback service and the interface.
 *
 * The service owns the real [Player]; the interface talks to it through this
 * object, which also mirrors the player into a [StateFlow] so Compose can
 * recompose without holding a controller connection of its own.
 */
object PlaybackController {

    @Volatile
    private var player: Player? = null

    @Volatile
    private var sleepTimerSetter: ((Int) -> Unit)? = null

    /** Messages the service wants surfaced to the user, e.g. a playback error. */
    @Volatile
    var messageSink: ((String) -> Unit)? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** True once the service is alive and a player exists. */
    val isReady: Boolean get() = player != null

    /** The player itself, needed by the embedded video view. */
    fun playerForVideo(): Player? = player

    /** Called by the service when it starts. */
    internal fun attach(player: Player, sleepTimer: (Int) -> Unit) {
        this.player = player
        this.sleepTimerSetter = sleepTimer
    }

    /** Called by the service when it stops. */
    internal fun reset() {
        player = null
        sleepTimerSetter = null
        _state.value = PlaybackState()
    }

    /** Replaces the queue with [tracks] and starts playing at [startIndex]. */
    fun play(tracks: List<MediaTrack>, startIndex: Int) {
        val p = player ?: return
        if (tracks.isEmpty()) return
        val index = startIndex.coerceIn(0, tracks.size - 1)
        val selected = tracks[index]
        p.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
        p.prepare()
        p.play()
        // Surface the title and video flag straight away instead of waiting for
        // the next player event, so tapping a video reveals the surface at once.
        _state.value = _state.value.copy(
            mediaKey = selected.stableKey,
            title = selected.title,
            subtitle = selected.subtitle,
            isVideo = selected.isVideo,
        )
    }

    /** Jumps to an item already in the queue. */
    fun playAt(index: Int) {
        val p = player ?: return
        if (index < 0 || index >= p.mediaItemCount) return
        p.seekTo(index, 0L)
        if (!p.isPlaying) p.play()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun next() {
        player?.seekToNextMediaItem()
    }

    fun previous() {
        player?.seekToPreviousMediaItem()
    }

    /** Moves [deltaMs] through the current item, clamped to its bounds. */
    fun skipBy(deltaMs: Long) {
        val p = player ?: return
        if (p.mediaItemCount == 0) return
        val duration = p.duration
        val target = p.currentPosition + deltaMs
        val clamped = when {
            target < 0L -> 0L
            duration > 0L && target > duration -> duration
            else -> target
        }
        p.seekTo(clamped)
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setShuffle(enabled: Boolean) {
        player?.shuffleModeEnabled = enabled
    }

    fun setRepeatMode(mode: Int) {
        player?.repeatMode = when (mode) {
            PlayerSettings.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            PlayerSettings.REPEAT_ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) {
        player?.volume = volume.coerceIn(0f, 1f)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerSetter?.invoke(minutes)
    }

    internal fun update(state: PlaybackState) {
        _state.value = state
    }

    internal fun postMessage(message: String) {
        messageSink?.invoke(message)
    }
}

/** Maps a library entry onto a Media3 item, including lock-screen metadata. */
fun MediaTrack.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist.ifBlank { if (isVideo) "Video" else null })
        .setAlbumTitle(album.ifBlank { null })
        .setArtworkUri(artworkUri)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()

    return MediaItem.Builder()
        .setMediaId(stableKey)
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}
