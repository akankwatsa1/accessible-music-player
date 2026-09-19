package com.akankwatsa.accessiblemusicplayer.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Videocam

import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.akankwatsa.accessiblemusicplayer.data.MediaTrack
import com.akankwatsa.accessiblemusicplayer.data.PlayerSettings
import com.akankwatsa.accessiblemusicplayer.playback.PlaybackController
import kotlinx.coroutines.launch

/**
 * The whole player interface.
 *
 * Layout, from top to bottom:
 *  - a compact bar that always keeps the collapse toggle and search reachable
 *  - optional detail area (search field, filters, count, scan state)
 *  - the video surface, shown only while a video is playing
 *  - the library list, which fills whatever space is left
 *  - the transport bar with previous, skip back 10s, play/pause,
 *    skip forward 10s and next
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current

    val uiState by viewModel.uiState.collectAsComposeState()
    val tracks by viewModel.visibleTracks.collectAsComposeState()
    val playback by viewModel.playback.collectAsComposeState()
    val settings by viewModel.settings.collectAsComposeState()
    val query by viewModel.searchQuery.collectAsComposeState()
    val filter by viewModel.filter.collectAsComposeState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsComposeState()
    val selectedArtist by viewModel.selectedArtist.collectAsComposeState()
    val scanning by viewModel.scanning.collectAsComposeState()
    val snackbar by viewModel.snackbar.collectAsComposeState()

    var searchOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<PickerKind?>(null) }
    var pendingDelete by remember { mutableStateOf<LongPressTarget?>(null) }
    var fullscreenVideo by remember { mutableStateOf(false) }

    // Surface service messages (for example a playback error) as a snackbar.
    DisposableEffect(Unit) {
        PlaybackController.messageSink = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
        onDispose { PlaybackController.messageSink = null }
    }

    LaunchedEffect(snackbar) {
        snackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Hand the permanent-delete request to Android, which confirms with the user.
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteFinished(result.resultCode == Activity.RESULT_OK)
        viewModel.clearDeleteRequest()
    }

    val deleteRequest by viewModel.deleteRequest.collectAsComposeState()
    LaunchedEffect(deleteRequest) {
        deleteRequest?.let { sender ->
            runCatching { deleteLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
                .onFailure { viewModel.clearDeleteRequest() }
        }
    }

    // Real full screen: hide the status and navigation bars while watching.
    DisposableEffect(fullscreenVideo) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (fullscreenVideo) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }

    // ------------------------------------------------------------ full screen
    // Watching a video full screen shows the video and the transport controls
    // only: no app bar, no library list and no system bars.
    if (fullscreenVideo && playback.isVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoSurface(
                zoomFill = settings.zoomFill,
                onToggleZoom = { viewModel.setZoomFill(!settings.zoomFill) },
            )
            TransportBar(
                playback = playback,
                onPrevious = { PlaybackController.previous() },
                onSkipBack = { viewModel.skipBack() },
                onPlayPause = { PlaybackController.togglePlayPause() },
                onSkipForward = { viewModel.skipForward() },
                onNext = { PlaybackController.next() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeat() },
                onToggleFullscreen = { fullscreenVideo = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The bar disappears entirely when the controls are hidden, so the list
        // and the video get the whole screen.
        topBar = {
            if (searchOpen) {
                TopBar(
                    detailsExpanded = settings.detailsExpanded,
                    onToggleDetails = { },
                    onToggleSearch = {
                        searchOpen = false
                        viewModel.clearSearch()
                    },
                    searchOpen = true,
                    query = query,
                    onQueryChange = viewModel::setSearchQuery,
                    onOpenMenu = { },
                )
            } else if (settings.detailsExpanded) {
                TopBar(
                    detailsExpanded = true,
                    onToggleDetails = { viewModel.setDetailsExpanded(false) },
                    onToggleSearch = { searchOpen = true },
                    searchOpen = false,
                    query = query,
                    onQueryChange = viewModel::setSearchQuery,
                    onOpenMenu = { menuOpen = true },
                )
            }
        },
        bottomBar = {
            TransportBar(
                playback = playback,
                onPrevious = { PlaybackController.previous() },
                onSkipBack = { viewModel.skipBack() },
                onPlayPause = { PlaybackController.togglePlayPause() },
                onSkipForward = { viewModel.skipForward() },
                onNext = { PlaybackController.next() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeat() },
                onToggleFullscreen = if (playback.isVideo) {
                    { fullscreenVideo = true }
                } else {
                    null
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (settings.detailsExpanded) {
                    DetailHeader(
                        filter = filter,
                        album = selectedAlbum,
                        artist = selectedArtist,
                        onFilterChange = { mode ->
                            when (mode) {
                                LibraryFilter.ALBUMS -> picker = PickerKind.ALBUM
                                LibraryFilter.ARTISTS -> picker = PickerKind.ARTIST
                                else -> viewModel.setFilter(mode)
                            }
                        },
                        onLongPress = { mode ->
                            when (mode) {
                                LibraryFilter.ALBUMS ->
                                    if (selectedAlbum.isNotBlank()) {
                                        pendingDelete = LongPressTarget.Album(selectedAlbum)
                                    }
                                LibraryFilter.ARTISTS ->
                                    if (selectedArtist.isNotBlank()) {
                                        pendingDelete = LongPressTarget.Artist(selectedArtist)
                                    }
                                else -> Unit
                            }
                        },
                        trackCount = tracks.size,
                        scanning = scanning,
                    )
                }

                if (playback.isVideo) {
                    VideoSurface(
                        zoomFill = settings.zoomFill,
                        onToggleZoom = { viewModel.setZoomFill(!settings.zoomFill) },
                    )
                }

                when (uiState) {
                    PlayerUiState.NeedsPermission -> PermissionRequest(
                        onGrant = { viewModel.onPermissionResult(true) },
                        onOpenSettings = {
                            viewModel.showMessage("Enable media access in app settings")
                        },
                    )

                    PlayerUiState.Loading -> LoadingState()
                    PlayerUiState.Empty -> EmptyState(
                        onRescan = { viewModel.rescan(announce = true) }
                    )

                    PlayerUiState.Ready -> {
                        if (tracks.isEmpty()) {
                            NoResultsState()
                        } else {
                            TrackList(
                                tracks = tracks,
                                playingKey = playback.mediaKey,
                                favourites = settings.favourites,
                                onPlay = viewModel::play,
                                onToggleFavourite = viewModel::toggleFavourite,
                                onLongPress = { pendingDelete = LongPressTarget.Track(it) },
                            )
                        }
                    }
                }
            }

            if (!settings.detailsExpanded) {
                SmallFloatingButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    description = "Show controls",
                    onClick = { viewModel.setDetailsExpanded(true) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
        }
    }

    if (menuOpen) {
        MoreOptionsSheet(
            settings = settings,
            playback = playback,
            onDismiss = { menuOpen = false },
            onOpenSettings = { settingsOpen = true },
            onRescan = { viewModel.rescan(announce = true) },
            onOpenHelp = { helpOpen = true },
        )
    }

    if (settingsOpen) {
        SettingsSheet(
            settings = settings,
            onDismiss = { settingsOpen = false },
            onSpeedChange = viewModel::setSpeed,
            onVolumeChange = viewModel::setVolume,
            onSleepTimer = viewModel::setSleepTimer,
            onZoomToggle = { viewModel.setZoomFill(!settings.zoomFill) },
        )
    }

    if (helpOpen) {
        HelpDialog(
            version = com.akankwatsa.accessiblemusicplayer.BuildConfig.VERSION_NAME,
            onDismiss = { helpOpen = false },
        )
    }

    picker?.let { kind ->
        val names = when (kind) {
            PickerKind.ALBUM -> viewModel.albumNames()
            PickerKind.ARTIST -> viewModel.artistNames()
        }
        SelectionSheet(
            title = if (kind == PickerKind.ALBUM) "Choose an album" else "Choose an artist",
            options = names,
            allLabel = if (kind == PickerKind.ALBUM) "All albums" else "All artists",
            onSelect = { value ->
                if (kind == PickerKind.ALBUM) viewModel.selectAlbum(value)
                else viewModel.selectArtist(value)
                picker = null
            },
            onDismiss = { picker = null },
        )
    }

    pendingDelete?.let { target ->
        ActionDialog(
            target = target,
            albumCount = if (target is LongPressTarget.Album) {
                viewModel.tracksOfAlbum(target.name).size
            } else {
                0
            },
            artistCount = if (target is LongPressTarget.Artist) {
                viewModel.tracksOfArtist(target.name).size
            } else {
                0
            },
            onRemoveFromList = {
                when (target) {
                    is LongPressTarget.Track -> viewModel.forgetTrack(target.track)
                    is LongPressTarget.Album -> viewModel.forgetAlbum(target.name)
                    is LongPressTarget.Artist -> viewModel.forgetArtist(target.name)
                }
                pendingDelete = null
            },
            onDeleteFromDevice = {
                val victims = when (target) {
                    is LongPressTarget.Track -> listOf(target.track)
                    is LongPressTarget.Album -> viewModel.tracksOfAlbum(target.name)
                    is LongPressTarget.Artist -> viewModel.tracksOfArtist(target.name)
                }
                viewModel.requestPermanentDelete(victims)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** What a long press was aimed at. */
private sealed interface LongPressTarget {
    data class Track(val track: MediaTrack) : LongPressTarget
    data class Album(val name: String) : LongPressTarget
    data class Artist(val name: String) : LongPressTarget
}

/** Which picker sheet is open. */
private enum class PickerKind { ALBUM, ARTIST }

/** Offers a non-destructive and a permanent way to get rid of a selection. */
@Composable
private fun ActionDialog(
    target: LongPressTarget,
    albumCount: Int,
    artistCount: Int,
    onRemoveFromList: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val what = when (target) {
        is LongPressTarget.Track -> target.track.title
        is LongPressTarget.Album -> "the album " + target.name + " (" + albumCount + " items)"
        is LongPressTarget.Artist -> "everything by " + target.name + " (" + artistCount + " items)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What should happen to " + what + "?") },
        text = {
            Text(
                "Remove from the list hides it here and nothing is lost - use Rescan " +
                    "library to bring it back.\n\nDelete from this device erases the " +
                    "file for good. Android will ask you to confirm."
            )
        },
        confirmButton = {
            TextButton(onClick = onRemoveFromList) { Text("Remove from list") }
        },
        dismissButton = {
            TextButton(onClick = onDeleteFromDevice) { Text("Delete from device") }
        },
    )
}

/** A small round button used while the app is in full-screen mode. */
@Composable
private fun SmallFloatingButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A sheet that lists albums or artists so one can be chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionSheet(
    title: String,
    options: List<String>,
    allLabel: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
            if (options.isEmpty()) {
                Text(
                    text = "Nothing to choose from yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item { SheetItem(allLabel) { onSelect("") } }
                items(items = options, key = { it }) { option ->
                    SheetItem(option) { onSelect(option) }
                }
            }
        }
    }
}

/** Reads a StateFlow as Compose state. */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsComposeState(): androidx.compose.runtime.State<T> =
    this.collectAsState()

/* ---------------------------------------------------------------- top bar */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    detailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
    onToggleSearch: () -> Unit,
    searchOpen: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenMenu: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            if (searchOpen) {
                val focusRequester = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current

                // Ask for focus and raise the keyboard as soon as search opens.
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Search music and video" },
                    singleLine = true,
                    placeholder = { Text("Search music and video") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                )
            } else {
                Text(
                    text = "Music Player",
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        actions = {
            if (!searchOpen) {
                // The declutter control: hides every extra panel for full screen.
                IconButton(onClick = onToggleDetails) {
                    Icon(
                        imageVector = if (detailsExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (detailsExpanded) "Hide controls for full screen"
                        else "Show controls",
                    )
                }
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                    )
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                    )
                }
            } else {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close search",
                    )
                }
            }
        },
    )
}

/* ------------------------------------------------------------ detail area */

@Composable
private fun DetailHeader(
    filter: LibraryFilter,
    album: String,
    artist: String,
    onFilterChange: (LibraryFilter) -> Unit,
    onLongPress: (LibraryFilter) -> Unit,
    trackCount: Int,
    scanning: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = filter == LibraryFilter.ALL,
                onClick = { onFilterChange(LibraryFilter.ALL) },
                label = { Text("All") },
            )
            FilterChip(
                selected = filter == LibraryFilter.FAVOURITES,
                onClick = { onFilterChange(LibraryFilter.FAVOURITES) },
                label = { Text("Favourites") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            FilterChip(
                selected = filter == LibraryFilter.ALBUMS,
                onClick = { onFilterChange(LibraryFilter.ALBUMS) },
                label = {
                    Text(if (album.isBlank()) "Albums" else album, maxLines = 1)
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            FilterChip(
                selected = filter == LibraryFilter.ARTISTS,
                onClick = { onFilterChange(LibraryFilter.ARTISTS) },
                label = {
                    Text(if (artist.isBlank()) "Artists" else artist, maxLines = 1)
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            FilterChip(
                selected = filter == LibraryFilter.VIDEOS,
                onClick = { onFilterChange(LibraryFilter.VIDEOS) },
                label = { Text("Video") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }

        Text(
            text = if (scanning) "Scanning device for music\u2026" else "$trackCount items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/* ---------------------------------------------------------------- video */

@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    zoomFill: Boolean,
    onToggleZoom: () -> Unit,
) {
    val player = PlaybackController.playerForVideo()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 300.dp)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggleZoom() })
            },
    ) {
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        controllerAutoShow = true
                        resizeMode = if (zoomFill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = if (zoomFill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
            )
        }
    }
}

/* ------------------------------------------------------------ transport */

@Composable
private fun TransportBar(
    playback: com.akankwatsa.accessiblemusicplayer.playback.PlaybackState,
    onPrevious: () -> Unit,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // What is playing, announced politely as it changes.
            Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    text = playback.title.ifBlank { "Nothing playing" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (playback.subtitle.isNotBlank()) {
                    Text(
                        text = playback.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SeekRow(
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                onSeek = onSeek,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = if (playback.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (playback.shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TransportIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    description = "Previous track",
                    enabled = playback.hasPrevious,
                    onClick = onPrevious,
                )
                SkipButton(isForward = false, onClick = onSkipBack)
                PlayPauseButton(isPlaying = playback.isPlaying, onClick = onPlayPause)
                SkipButton(isForward = true, onClick = onSkipForward)
                TransportIconButton(
                    icon = Icons.Filled.SkipNext,
                    description = "Next track",
                    enabled = playback.hasNext,
                    onClick = onNext,
                )
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (playback.repeatMode == PlayerSettings.REPEAT_ONE) Icons.Filled.RepeatOne
                        else Icons.Filled.Repeat,
                        contentDescription = when (playback.repeatMode) {
                            PlayerSettings.REPEAT_ALL -> "Repeat all"
                            PlayerSettings.REPEAT_ONE -> "Repeat one"
                            else -> "Repeat off"
                        },
                        tint = if (playback.repeatMode != PlayerSettings.REPEAT_OFF)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onToggleFullscreen != null) {
                    IconButton(onClick = onToggleFullscreen) {
                        Icon(
                            imageVector = if (playback.isVideo) Icons.Filled.Fullscreen
                            else Icons.Filled.FullscreenExit,
                            contentDescription = if (playback.isVideo) {
                                "Watch full screen"
                            } else {
                                "Leave full screen"
                            },
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** Position slider with spoken value text in minutes and seconds. */
@Composable
private fun SeekRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }

    val max = durationMs.coerceAtLeast(1L).toFloat()
    val shown = if (dragging) dragValue else positionMs.toFloat().coerceIn(0f, max)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = shown,
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    dragging = false
                    onSeek(dragValue.toLong())
                },
                valueRange = 0f..max,
                enabled = durationMs > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Playback position"
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = positionMs.toFloat(),
                            range = 0f..max,
                            steps = 0,
                        )
                        customActions = listOf(
                            CustomAccessibilityAction("Skip back 10 seconds") {
                                onSeek((positionMs - 10_000L).coerceAtLeast(0L)); true
                            },
                            CustomAccessibilityAction("Skip forward 10 seconds") {
                                onSeek((positionMs + 10_000L).coerceAtMost(durationMs)); true
                            },
                        )
                    },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(shown.toLong()), style = MaterialTheme.typography.labelLarge)
            Text(formatTime(durationMs), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/** A seek button that shows the number of seconds it jumps. */
@Composable
private fun SkipButton(isForward: Boolean, onClick: () -> Unit) {
    val description = if (isForward) "Skip forward 10 seconds" else "Skip back 10 seconds"
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isForward) Icons.Filled.SkipNext
                else Icons.Filled.SkipPrevious,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "10",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TransportIconButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(30.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

/* ------------------------------------------------------------ track list */

@Composable
private fun TrackList(
    tracks: List<MediaTrack>,
    playingKey: String,
    favourites: Set<String>,
    onPlay: (MediaTrack) -> Unit,
    onToggleFavourite: (MediaTrack) -> Unit,
    onLongPress: (MediaTrack) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = tracks, key = { it.stableKey }) { track ->
            TrackRow(
                track = track,
                isPlaying = track.stableKey == playingKey,
                isFavourite = favourites.contains(track.stableKey),
                onPlay = { onPlay(track) },
                onToggleFavourite = { onToggleFavourite(track) },
                onLongPress = { onLongPress(track) },
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun TrackRow(
    track: MediaTrack,
    isPlaying: Boolean,
    isFavourite: Boolean,
    onPlay: () -> Unit,
    onToggleFavourite: () -> Unit,
    onLongPress: () -> Unit,
) {
    val spoken = buildString {
        append(track.title)
        if (track.artist.isNotBlank()) append(", by ").append(track.artist)
        if (track.album.isNotBlank()) append(", from ").append(track.album)
        if (track.isVideo) append(", video")
        if (isFavourite) append(", favourite")
        if (isPlaying) append(", now playing")
    }

    Surface(
        onClick = onPlay,
        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                selected = isPlaying
                customActions = listOf(
                    CustomAccessibilityAction(
                        if (isFavourite) "Remove from favourites" else "Add to favourites"
                    ) { onToggleFavourite(); true },
                    CustomAccessibilityAction("Remove or delete this track") { onLongPress(); true },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (track.isVideo) Icons.Filled.Videocam else Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.subtitle.isNotBlank()) {
                    Text(
                        text = track.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatTime(track.durationMs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onLongPress,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove or delete " + track.title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* --------------------------------------------------------------- states */

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Scanning device for music\u2026",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun EmptyState(onRescan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No music or video found on this device.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Copy some songs to this device, then rescan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRescan) { Text("Rescan library") }
    }
}

@Composable
private fun NoResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No tracks match your search.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun PermissionRequest(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Allow access to your music?",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Music Player scans this device for audio and video files so you can play them. " +
                "Your files stay on the device and nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onGrant, modifier = Modifier.heightIn(min = 52.dp)) {
            Text("Allow access")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) { Text("Open app settings") }
    }
}

/* ---------------------------------------------------------- other sheets */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsSheet(
    settings: PlayerSettings,
    playback: com.akankwatsa.accessiblemusicplayer.playback.PlaybackState,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onRescan: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "More options",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .semantics { heading() },
            )
            SheetItem("Playback settings and sleep timer", onOpenSettings)
            SheetItem("Rescan library", onRescan)
            SheetItem("Help and gestures", onOpenHelp)
            HorizontalDivider()
            Text(
                text = if (playback.mediaKey.isBlank()) "Nothing playing"
                else "Playing: ${playback.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: PlayerSettings,
    onDismiss: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onZoomToggle: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Playback settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            Text("Playback speed", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = settings.playbackSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..3f,
                modifier = Modifier.semantics {
                    contentDescription = "Playback speed"
                },
            )
            Text(
                text = String.format("%.2f times speed", settings.playbackSpeed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { onSpeedChange(1f) }) { Text("Reset speed to normal") }

            HorizontalDivider()
            Text("Volume", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = settings.volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.semantics { contentDescription = "Volume" },
            )

            HorizontalDivider()
            Text("Sleep timer", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 5, 15, 30, 60).forEach { minutes ->
                    FilterChip(
                        selected = false,
                        onClick = { onSleepTimer(minutes) },
                        label = { Text(if (minutes == 0) "Off" else "${minutes}m") },
                    )
                }
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onZoomToggle, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (settings.zoomFill) "Video zoom: fill screen" else "Video zoom: fit picture")
                }
            }
        }
    }
}

@Composable
private fun SheetItem(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HelpDialog(version: String, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help and gestures") },
        text = {
            Text(
                "Tap a track to play it. Use the buttons at the bottom to play or pause, " +
                    "move between tracks, and jump back or forward 10 seconds. Drag the " +
                    "position slider to seek. Tap the chevron at the top right to hide or " +
                    "show the extra controls so only search and the library remain. Use the " +
                    "heart on a track to add it to favourites. Double tap the video to switch " +
                    "between fitting and filling the screen.\n\n" +
                    "Accessible Music Player $version"
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/* ---------------------------------------------------------------- helpers */

/** Formats milliseconds as m:ss, or h:mm:ss for long items. */
internal fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
