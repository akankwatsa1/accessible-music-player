package com.akankwatsa.accessiblemusicplayer

import android.app.Application
import com.akankwatsa.accessiblemusicplayer.data.MediaLibraryRepository
import com.akankwatsa.accessiblemusicplayer.data.SettingsRepository

/**
 * Holds the two long-lived repositories. The app deliberately avoids a
 * dependency injection framework to stay small and fast to start.
 */
class MusicPlayerApp : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val libraryRepository: MediaLibraryRepository by lazy { MediaLibraryRepository(this) }

    companion object {
        lateinit var instance: MusicPlayerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}