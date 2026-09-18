package com.akankwatsa.accessiblemusicplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device for every playable audio and video file using MediaStore.
 *
 * This replaces the desktop app's recursive folder walker: on Android the
 * platform index is the authoritative list of media on the device, and it
 * keeps itself up to date as files are added or removed.
 */
class MediaLibraryRepository(private val context: Context) {

    /**
     * Returns every track on the device, audio first and each group sorted by
     * title. Very short audio clips (under a second) are skipped, because they
     * are notification and system sounds rather than music.
     */
    suspend fun scanAll(): List<MediaTrack> = withContext(Dispatchers.IO) {
        val tracks = ArrayList<MediaTrack>(256)
        runCatching { tracks.addAll(queryAudio()) }
        runCatching { tracks.addAll(queryVideo()) }
        tracks.sortWith(
            compareBy<MediaTrack> { it.isVideo }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
        tracks
    }

    private fun queryAudio(): List<MediaTrack> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
        )

        val selection = "${MediaStore.Audio.Media.DURATION} IS NULL OR " +
            "${MediaStore.Audio.Media.DURATION} >= 1000"
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val out = ArrayList<MediaTrack>()
        context.contentResolver.query(collection, projection, selection, null, order)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: c.getString(nameCol).orEmpty()
                val artist = cleanUnknown(c.getString(artistCol))
                val album = c.getString(albumCol).orEmpty()
                val albumId = c.getLong(albumIdCol)
                out.add(
                    MediaTrack(
                        id = id,
                        uri = uri,
                        title = title.ifBlank { "Unknown track" },
                        artist = artist,
                        album = album,
                        durationMs = c.getLong(durCol),
                        isVideo = false,
                        artworkUri = albumArtUri(albumId),
                        subtitle = joinSubtitle(artist, album),
                        relativePath = c.getString(pathCol).orEmpty(),
                    )
                )
            }
        }
        return out
    }

    private fun queryVideo(): List<MediaTrack> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RELATIVE_PATH,
        )
        val order = "${MediaStore.Video.Media.TITLE} COLLATE NOCASE ASC"

        val out = ArrayList<MediaTrack>()
        context.contentResolver.query(collection, projection, null, null, order)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: c.getString(nameCol).orEmpty()
                out.add(
                    MediaTrack(
                        id = id,
                        uri = uri,
                        title = title.ifBlank { "Unknown video" },
                        artist = "",
                        album = "",
                        durationMs = c.getLong(durCol),
                        isVideo = true,
                        artworkUri = videoThumbUri(id),
                        subtitle = "Video",
                        relativePath = c.getString(pathCol).orEmpty(),
                    )
                )
            }
        }
        return out
    }

    private fun albumArtUri(albumId: Long): Uri? {
        if (albumId <= 0L) return null
        return runCatching {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
        }.getOrNull()
    }

    private fun videoThumbUri(id: Long): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath("thumbnail").build(),
                id
            )
        }.getOrNull()
    }

    private fun cleanUnknown(value: String?): String {
        val v = value.orEmpty()
        return if (v.equals("<unknown>", ignoreCase = true)) "" else v
    }

    private fun joinSubtitle(artist: String, album: String): String = when {
        artist.isNotBlank() && album.isNotBlank() -> "$artist \u2014 $album"
        artist.isNotBlank() -> artist
        album.isNotBlank() -> album
        else -> ""
    }
}