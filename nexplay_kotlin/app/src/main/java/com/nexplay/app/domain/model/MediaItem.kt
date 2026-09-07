package com.nexplay.app.domain.model

enum class MediaType {
    AUDIO,
    VIDEO,
}

sealed interface MediaItem {
    val id: String
    val uri: String
    val displayName: String
    val title: String
    val durationMs: Long?
    val mimeType: String?
    val folder: FolderInfo?
    val sizeBytes: Long?
    val dateAddedEpochSeconds: Long?
    val dateModifiedEpochSeconds: Long?
    val mediaType: MediaType
}

data class AudioItem(
    override val id: String,
    override val uri: String,
    override val displayName: String,
    override val title: String,
    override val durationMs: Long?,
    override val mimeType: String?,
    override val folder: FolderInfo?,
    override val sizeBytes: Long?,
    override val dateAddedEpochSeconds: Long?,
    override val dateModifiedEpochSeconds: Long?,
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val track: Int?,
) : MediaItem {
    override val mediaType: MediaType = MediaType.AUDIO
}

data class VideoItem(
    override val id: String,
    override val uri: String,
    override val displayName: String,
    override val title: String,
    override val durationMs: Long?,
    override val mimeType: String?,
    override val folder: FolderInfo?,
    override val sizeBytes: Long?,
    override val dateAddedEpochSeconds: Long?,
    override val dateModifiedEpochSeconds: Long?,
    val width: Int?,
    val height: Int?,
) : MediaItem {
    override val mediaType: MediaType = MediaType.VIDEO

    val resolution: String?
        get() {
            val resolvedWidth = width ?: return null
            val resolvedHeight = height ?: return null

            return "${resolvedWidth}x${resolvedHeight}"
        }
}