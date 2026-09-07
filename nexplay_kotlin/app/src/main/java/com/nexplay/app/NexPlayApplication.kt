package com.nexplay.app

import android.app.Application
import com.nexplay.app.data.playlist.NexPlayDatabase
import com.nexplay.app.data.playlist.PlaylistRepository
import com.nexplay.app.data.preferences.NexPlayPreferencesRepository
import com.nexplay.app.playback.QueuePlaybackCoordinator

class NexPlayApplication : Application() {
    val playbackCoordinator:
            QueuePlaybackCoordinator by lazy {
        QueuePlaybackCoordinator(
            applicationContext,
        )
    }

    val preferencesRepository:
            NexPlayPreferencesRepository by lazy {
        NexPlayPreferencesRepository(
            applicationContext,
        )
    }

    private val database:
            NexPlayDatabase by lazy {
        NexPlayDatabase.create(
            applicationContext,
        )
    }

    val playlistRepository:
            PlaylistRepository by lazy {
        PlaylistRepository(
            database,
        )
    }
}
