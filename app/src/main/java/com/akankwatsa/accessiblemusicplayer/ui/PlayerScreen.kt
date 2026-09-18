package com.akankwatsa.accessiblemusicplayer.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
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

import androidx.compose.material.icons.automirrored.filled.SkipNext
import androidx.compose.material.icons.automirrored.filled.SkipPrevious
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsComposeState()
    val tracks by viewModel.visibleTracks.collectAsComposeState()
    val playback by viewModel.playback.collectAsComposeState()
    val settings by viewModel.settings.collectAsComposeState()
    val query by viewModel.searchQuery.collectAsComposeState()
    val filter by viewModel.filter.collectAsComposeState()
    val scanning by viewModel.scanning.collectAsComposeState()
    val snackbar by viewModel.snackbar.collectAsComposeState()

    var searchOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                detailsExpanded = settings.detailsExpanded,
                onToggleDetails = { viewModel.setDetailsExpanded(!settings.detailsExpanded) },
                onToggleSearch = {
                    searchOpen = !searchOpen
                    if (!searchOpen) viewModel.clearSearch()
                },
                searchOpen = searchOpen,
                onOpenMenu = { menuOpen = true },
            )
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
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (settings.detailsExpanded || searchOpen) {
                DetailHeader(
                    query = query,
                    onQueryChange = viewModel::setSearchQuery,
                    searchOpen = searchOpen,
                    onCloseSearch = {
                        searchOpen = false
                        viewModel.clearSearch()
                    },
                    filter = filter,
                    onFilterChange = viewModel::setFilter,
                    trackCount = tracks.size,
                    scanning = scanning,
                    favouritesOnly = settings.favouritesOnly,
                )
            }

            if (playback.isVideo) {
                VideoSurface(
                    zoomFill = settings.zoomFill,
                    onToggleZoom = { viewModel.setZoomFill(!settings.zoomFill) },
                )
            }

            // Body: permission prompt, scan progress, empty state, or the list.
            when (uiState) {
                PlayerUiState.NeedsPermission -> PermissionRequest(
                    onGrant = { viewModel.onPermissionResult(true) },
                    onOpenSettings = { viewModel.showMessage("Enable media access in app settings") },
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
                        )
                    }
                }
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
    onOpenMenu: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                text = "Music Player",
                modifier = Modifier.semantics { heading() },
            )
        },
        actions = {
            // The declutter control, kept in the top right corner as requested.
            IconButton(onClick = onToggleDetails) {
                Icon(
                    imageVector = if (detailsExpanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (detailsExpanded) "Hide extra controls"
                    else "Show extra controls",
                )
            }
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (searchOpen) "Close search" else "Search",
                )
            }
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                )
            }
        },
    )
}

/* ------------------------------------------------------------ detail area */

@Composable
private fun DetailHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    searchOpen: Boolean,
    onCloseSearch: () -> Unit,
    filter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit,
    trackCount: Int,
    scanning: Boolean,
    favouritesOnly: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
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

        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            FilterChip(
                selected = filter == LibraryFilter.VIDEOS,
                onClick = { onFilterChange(LibraryFilter.VIDEOS) },
                label = { Text("Video") },
                leadingIcon = {
                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }

        Text(
            text = if (scanning) "Scanning device for music\u2026"
            else "$trackCount items",
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
) {
    Surface(
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
                    icon = Icons.AutoMirrored.Filled.SkipPrevious,
                    description = "Previous track",
                    enabled = playback.hasPrevious,
                    onClick = onPrevious,
                )
                SkipButton(isForward = false, onClick = onSkipBack)
                PlayPauseButton(isPlaying = playback.isPlaying, onClick = onPlayPause)
                SkipButton(isForward = true, onClick = onSkipForward)
                TransportIconButton(
                    icon = Icons.AutoMirrored.Filled.SkipNext,
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
                imageVector = if (isForward) Icons.AutoMirrored.Filled.SkipNext
                else Icons.AutoMirrored.Filled.SkipPrevious,
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
) {
    val spoken = buildString {
        append(track.title)
        if (track.artist.isNotBlank()) append(", by ").append(track.artist)
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
                    ) { onToggleFavourite(); true }
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatTime(track.durationMs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavourite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
