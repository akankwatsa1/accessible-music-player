package com.akankwatsa.accessiblemusicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.akankwatsa.accessiblemusicplayer.MainActivity
import com.akankwatsa.accessiblemusicplayer.MusicPlayerApp
import com.akankwatsa.accessiblemusicplayer.R
import com.akankwatsa.accessiblemusicplayer.data.PlayerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the one and only [ExoPlayer] instance.
 *
 * Running the player inside a MediaSessionService is what keeps audio alive
 * when the screen locks or the app is backgrounded, and it is what places the
 * transport controls on the lock screen, in the notification shade and on
 * Bluetooth headsets. The media notification and its seek buttons are produced
 * automatically by MediaSessionService from the session below, so no manual
 * notification code is needed.
 */
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null
    private var sleepJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        val settingsRepository = (application as MusicPlayerApp).settingsRepository
        val libraryRepository = (application as MusicPlayerApp).libraryRepository

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            // Pause when headphones are unplugged instead of blasting the speaker.
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player = exoPlayer
        PlaybackController.attach(
            player = exoPlayer,
            sleepTimer = { minutes -> setSleepTimer(minutes) }
        )

        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                publishState(p)
            }

            override fun onPlayerError(error: PlaybackException) {
                PlaybackController.postMessage(getString(R.string.announce_load_error))
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()

        scope.launch {
            val saved = settingsRepository.settings.first()

            exoPlayer.shuffleModeEnabled = saved.shuffle
            exoPlayer.repeatMode = when (saved.repeatMode) {
                PlayerSettings.REPEAT_ALL -> Player.REPEAT_MODE_ALL
                PlayerSettings.REPEAT_ONE -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            exoPlayer.setPlaybackSpeed(saved.playbackSpeed)
            exoPlayer.volume = saved.volume
            publishState(exoPlayer)

            // Rebuild the queue around the track that was playing last time.
            if (saved.lastMediaKey.isNotBlank()) {
                val library = libraryRepository.scanAll()
                val index = library.indexOfFirst { it.stableKey == saved.lastMediaKey }
                if (index >= 0) {
                    exoPlayer.setMediaItems(
                        library.map { it.toMediaItem() },
                        index,
                        saved.lastPositionMs.coerceAtLeast(0L)
                    )
                    exoPlayer.prepare()
                    publishState(exoPlayer)
                }
            }
        }

        // Persist the position periodically so playback can resume later.
        positionJob = scope.launch {
            while (true) {
                delay(POSITION_SAVE_INTERVAL_MS)
                val p = player ?: continue
                val item = p.currentMediaItem ?: continue
                if (p.currentPosition > 0L) {
                    settingsRepository.rememberLastPlayed(item.mediaId, p.currentPosition)
                }
            }
        }
    }

    /** Starts the sleep timer, or cancels it when [minutes] is zero. */
    private fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) return
        sleepJob = scope.launch {
            var remainingSeconds = minutes * 60L
            while (remainingSeconds > 0L) {
                delay(1000L)
                remainingSeconds--
            }
            player?.pause()
        }
    }

    private fun publishState(p: Player) {
        val metadata = p.mediaMetadata
        PlaybackController.update(
            PlaybackState(
                connected = true,
                isPlaying = p.isPlaying,
                isBuffering = p.playbackState == Player.STATE_BUFFERING,
                mediaKey = p.currentMediaItem?.mediaId.orEmpty(),
                title = metadata.title?.toString().orEmpty(),
                subtitle = metadata.artist?.toString().orEmpty(),
                isVideo = currentItemIsVideo(p),
                hasPrevious = p.hasPreviousMediaItem(),
                hasNext = p.hasNextMediaItem(),
                positionMs = p.currentPosition.coerceAtLeast(0L),
                durationMs = p.duration.takeIf { it > 0L } ?: 0L,
                shuffle = p.shuffleModeEnabled,
                repeatMode = when (p.repeatMode) {
                    Player.REPEAT_MODE_ALL -> PlayerSettings.REPEAT_ALL
                    Player.REPEAT_MODE_ONE -> PlayerSettings.REPEAT_ONE
                    else -> PlayerSettings.REPEAT_OFF
                },
                speed = p.playbackParameters.speed,
            )
        )
    }

    /** Video items are tagged by the library with an artist of "Video". */
    private fun currentItemIsVideo(p: Player): Boolean {
        val uri = p.currentMediaItem?.localConfiguration?.uri ?: return false
        return (contentResolver.getType(uri) ?: "").startsWith("video/")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        positionJob?.cancel()
        sleepJob?.cancel()
        scope.cancel()
        val currentSession = session
        val currentPlayer = player
        session = null
        player = null
        runCatching {
            currentSession?.release()
            currentPlayer?.release()
        }
        PlaybackController.reset()
        super.onDestroy()
    }

    private companion object {
        const val POSITION_SAVE_INTERVAL_MS = 5000L
    }
}
