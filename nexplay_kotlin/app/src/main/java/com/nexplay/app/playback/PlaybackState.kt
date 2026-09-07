package com.nexplay.app.playback

import com.nexplay.app.domain.model.MediaItem

enum class PlaybackStatus {
    IDLE,
    PREPARING,
    BUFFERING,
    READY,
    ENDED,
    ERROR,
}

enum class PlaybackErrorKind {
    SOURCE,
    RUNTIME,
}

data class PlaybackError(
    val kind: PlaybackErrorKind,
    val message: String,
    val diagnosticCode: String? = null,
)

data class PlaybackState(
    val currentMedia: MediaItem? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val error: PlaybackError? = null,
    val abRepeatStartMs: Long? = null,
    val abRepeatEndMs: Long? = null,
) {
    val isBuffering: Boolean
        get() = status == PlaybackStatus.BUFFERING

    val isAbRepeatEnabled: Boolean
        get() =
            abRepeatStartMs != null &&
                    abRepeatEndMs != null
}
