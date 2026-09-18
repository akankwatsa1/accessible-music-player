package com.akankwatsa.accessiblemusicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.akankwatsa.accessiblemusicplayer.playback.PlaybackService
import com.akankwatsa.accessiblemusicplayer.ui.PlayerScreen
import com.akankwatsa.accessiblemusicplayer.ui.PlayerViewModel
import com.akankwatsa.accessiblemusicplayer.ui.theme.AccessibleMusicPlayerTheme
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : ComponentActivity() {

    /** A media file handed to us by another app, played once the UI is ready. */
    private var pendingMediaUri by mutableStateOf<Uri?>(null)

    /** Bumped every time the activity resumes so permission state is rechecked. */
    private var resumeTick by mutableStateOf(0)

    private var controller: MediaController? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* The resume tick rechecks the result. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Starting the service here means the player exists for the whole
        // session and keeps playing when the screen locks.
        startService(Intent(this, PlaybackService::class.java))

        handleIntent(intent)

        setContent {
            AccessibleMusicPlayerTheme {
                val viewModel: PlayerViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )

                // Ask once, on first launch, for media access.
                LaunchedEffect(Unit) {
                    if (!hasMediaPermission()) {
                        permissionLauncher.launch(requiredPermissions())
                    }
                }

                // Re-check on every resume so returning from system settings
                // immediately unlocks the library.
                LaunchedEffect(resumeTick) {
                    val granted = hasMediaPermission()
                    viewModel.refreshPermissionState(granted)
                    if (granted) viewModel.rescan()
                }

                LaunchedEffect(pendingMediaUri) {
                    val uri = pendingMediaUri
                    if (uri != null) {
                        viewModel.playExternal(uri)
                        pendingMediaUri = null
                    }
                }

                PlayerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onStart() {
        super.onStart()
        bindPlaybackController()
    }

    override fun onStop() {
        controller?.release()
        controller = null
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        intent.data?.let { pendingMediaUri = it }
    }

    /** Connects to the running service so its player survives screen lock. */
    private fun bindPlaybackController() {
        if (controller != null) return
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            MoreExecutors.directExecutor()
        )
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** Audio access is what the library needs; required on every release. */
    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}