package com.akankwatsa.accessiblemusicplayer.data

import android.net.Uri

/**
 * One playable item found in the device media library.
 *
 * Every field is filled from MediaStore, so no file-system path is ever
 * required and the app works with scoped storage on every supported release.
 */
data class MediaTrack(
    /** MediaStore id, used as the stable identity of the track. */
    val id: Long,
    /** Playable content URI. */
    val uri: Uri,
    /** Track title, already falling back to the display name. */
    val title: String,
    /** Artist, or the album artist, or empty. */
    val artist: String,
    val album: String,
    /** Duration in milliseconds, or 0 when unknown. */
    val durationMs: Long,
    val isVideo: Boolean,
    /** Artwork or video thumbnail URI, when the platform can supply one. */
    val artworkUri: Uri?,
    /** Human readable second line for the list row. */
    val subtitle: String,
    /** Directory as reported by MediaStore, used for sorting and display. */
    val relativePath: String,
) {
    val stableKey: String get() = (if (isVideo) "v" else "a") + id
}