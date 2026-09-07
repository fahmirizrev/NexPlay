package com.nexplay.app.domain.queue

import com.nexplay.app.domain.model.MediaItem

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

enum class QueueCompletionAction {
    STOP,
    REPLAY_CURRENT,
    ADVANCED,
}

data class QueueState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackContext: PlaybackContext? = null,
) {
    val currentItem: MediaItem?
        get() = items.getOrNull(currentIndex)

    val hasPrevious: Boolean
        get() =
            currentIndex > 0 &&
                    currentIndex < items.size

    val hasNext: Boolean
        get() =
            currentIndex >= 0 &&
                    currentIndex < items.lastIndex
}