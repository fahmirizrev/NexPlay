package com.nexplay.app.domain.queue

import com.nexplay.app.domain.model.FolderInfo

sealed interface PlaybackContext

data class FolderContext(
    val folder: FolderInfo,
) : PlaybackContext

data class PlaylistContext(
    val playlistId: String,
    val playlistName: String,
) : PlaybackContext

data class SearchContext(
    val query: String,
    val sourceFolder: FolderInfo? = null,
) : PlaybackContext

data class PlayHubContext(
    val section: String,
) : PlaybackContext

data object SingleItemContext : PlaybackContext