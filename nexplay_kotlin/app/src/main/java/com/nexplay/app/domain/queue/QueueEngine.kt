package com.nexplay.app.domain.queue

import com.nexplay.app.domain.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class QueueEngine internal constructor(
    private val random: Random,
) {
    constructor() : this(
        random = Random.Default,
    )

    private val _state =
        MutableStateFlow(
            QueueState(),
        )

    val state: StateFlow<QueueState> =
        _state.asStateFlow()

    private var queueBeforeShuffle:
            List<MediaItem>? = null

    @Synchronized
    fun setQueue(
        items: List<MediaItem>,
        currentIndex: Int,
        playbackContext: PlaybackContext,
    ) {
        val currentState = _state.value

        if (items.isEmpty()) {
            queueBeforeShuffle = null

            _state.value =
                currentState.copy(
                    items = emptyList(),
                    currentIndex = -1,
                    playbackContext = null,
                )

            return
        }

        val queue = items.toList()
        val safeIndex =
            currentIndex.coerceIn(
                queue.indices,
            )

        if (
            currentState.shuffleEnabled &&
            queue.size > 1
        ) {
            queueBeforeShuffle = queue

            val selectedItem =
                queue[safeIndex]

            val remainingItems =
                queue.toMutableList().apply {
                    removeAt(safeIndex)
                    shuffle(random)
                }

            _state.value =
                currentState.copy(
                    items =
                        listOf(selectedItem) +
                                remainingItems,
                    currentIndex = 0,
                    playbackContext =
                        playbackContext,
                )

            return
        }

        queueBeforeShuffle = null

        _state.value =
            currentState.copy(
                items = queue,
                currentIndex = safeIndex,
                playbackContext =
                    playbackContext,
            )
    }

    @Synchronized
    fun playAt(
        index: Int,
    ) {
        val currentState = _state.value

        if (index !in currentState.items.indices) {
            return
        }

        _state.value =
            currentState.copy(
                currentIndex = index,
            )
    }

    @Synchronized
    fun next() {
        val currentState = _state.value

        val nextIndex =
            when {
                currentState.hasNext ->
                    currentState.currentIndex + 1

                currentState.repeatMode ==
                        RepeatMode.ALL &&
                        currentState.items.isNotEmpty() ->
                    0

                else ->
                    return
            }

        _state.value =
            currentState.copy(
                currentIndex = nextIndex,
            )
    }

    @Synchronized
    fun previous() {
        val currentState = _state.value

        val previousIndex =
            when {
                currentState.hasPrevious ->
                    currentState.currentIndex - 1

                currentState.repeatMode ==
                        RepeatMode.ALL &&
                        currentState.items.isNotEmpty() ->
                    currentState.items.lastIndex

                else ->
                    return
            }

        _state.value =
            currentState.copy(
                currentIndex = previousIndex,
            )
    }

    @Synchronized
    fun advanceAfterCompletion():
            QueueCompletionAction {
        val currentState = _state.value

        if (currentState.currentItem == null) {
            return QueueCompletionAction.STOP
        }

        if (
            currentState.repeatMode ==
            RepeatMode.ONE
        ) {
            return QueueCompletionAction.REPLAY_CURRENT
        }

        if (currentState.hasNext) {
            _state.value =
                currentState.copy(
                    currentIndex =
                        currentState.currentIndex + 1,
                )

            return QueueCompletionAction.ADVANCED
        }

        if (
            currentState.repeatMode ==
            RepeatMode.ALL
        ) {
            if (currentState.items.size == 1) {
                return QueueCompletionAction.REPLAY_CURRENT
            }

            _state.value =
                currentState.copy(
                    currentIndex = 0,
                )

            return QueueCompletionAction.ADVANCED
        }

        return QueueCompletionAction.STOP
    }

    @Synchronized
    fun append(
        item: MediaItem,
    ) {
        val currentState = _state.value
        val wasEmpty =
            currentState.items.isEmpty()

        invalidateShuffleRestore(
            currentState,
        )

        _state.value =
            currentState.copy(
                items =
                    currentState.items +
                            item,
                currentIndex =
                    if (wasEmpty) {
                        0
                    } else {
                        currentState.currentIndex
                    },
                playbackContext =
                    if (wasEmpty) {
                        SingleItemContext
                    } else {
                        currentState.playbackContext
                    },
            )
    }

    @Synchronized
    fun insertNext(
        item: MediaItem,
    ) {
        val currentState = _state.value

        if (currentState.items.isEmpty()) {
            invalidateShuffleRestore(
                currentState,
            )

            _state.value =
                currentState.copy(
                    items = listOf(item),
                    currentIndex = 0,
                    playbackContext =
                        SingleItemContext,
                )

            return
        }

        val insertIndex =
            (currentState.currentIndex + 1)
                .coerceIn(
                    0,
                    currentState.items.size,
                )

        val nextItems =
            currentState.items
                .toMutableList()
                .apply {
                    add(
                        insertIndex,
                        item,
                    )
                }

        invalidateShuffleRestore(
            currentState,
        )

        _state.value =
            currentState.copy(
                items = nextItems,
            )
    }

    @Synchronized
    fun remove(
        index: Int,
    ) {
        val currentState = _state.value

        if (index !in currentState.items.indices) {
            return
        }

        val currentItem =
            currentState.currentItem

        val nextItems =
            currentState.items
                .toMutableList()
                .apply {
                    removeAt(index)
                }

        invalidateShuffleRestore(
            currentState,
        )

        if (nextItems.isEmpty()) {
            _state.value =
                currentState.copy(
                    items = emptyList(),
                    currentIndex = -1,
                    playbackContext = null,
                )

            return
        }

        val nextCurrentIndex =
            if (
                index ==
                currentState.currentIndex
            ) {
                index.coerceAtMost(
                    nextItems.lastIndex,
                )
            } else {
                currentItem
                    ?.let { item ->
                        nextItems.indexOfFirst {
                                candidate ->
                            isSameMediaItem(
                                candidate,
                                item,
                            )
                        }
                    }
                    ?.takeIf { resolvedIndex ->
                        resolvedIndex >= 0
                    }
                    ?: currentState
                        .currentIndex
                        .coerceIn(
                            nextItems.indices,
                        )
            }

        _state.value =
            currentState.copy(
                items = nextItems,
                currentIndex =
                    nextCurrentIndex,
            )
    }

    @Synchronized
    fun move(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val currentState = _state.value

        if (
            fromIndex !in
            currentState.items.indices ||
            toIndex !in
            currentState.items.indices ||
            fromIndex == toIndex
        ) {
            return
        }

        val currentItem =
            currentState.currentItem

        val nextItems =
            currentState.items
                .toMutableList()
                .apply {
                    val movedItem =
                        removeAt(fromIndex)

                    add(
                        toIndex,
                        movedItem,
                    )
                }

        invalidateShuffleRestore(
            currentState,
        )

        val nextCurrentIndex =
            currentItem
                ?.let { item ->
                    nextItems.indexOfFirst {
                            candidate ->
                        isSameMediaItem(
                            candidate,
                            item,
                        )
                    }
                }
                ?.takeIf { resolvedIndex ->
                    resolvedIndex >= 0
                }
                ?: currentState
                    .currentIndex
                    .coerceIn(
                        nextItems.indices,
                    )

        _state.value =
            currentState.copy(
                items = nextItems,
                currentIndex =
                    nextCurrentIndex,
            )
    }

    @Synchronized
    fun clear() {
        val currentState = _state.value

        queueBeforeShuffle = null

        _state.value =
            currentState.copy(
                items = emptyList(),
                currentIndex = -1,
                playbackContext = null,
            )
    }

    @Synchronized
    fun toggleShuffle() {
        val currentState = _state.value
        val currentItem =
            currentState.currentItem

        if (
            currentItem == null ||
            currentState.items.size < 2
        ) {
            queueBeforeShuffle = null

            _state.value =
                currentState.copy(
                    shuffleEnabled =
                        !currentState
                            .shuffleEnabled,
                )

            return
        }

        if (currentState.shuffleEnabled) {
            val originalQueue =
                queueBeforeShuffle

            queueBeforeShuffle = null

            if (
                originalQueue != null &&
                originalQueue.isNotEmpty()
            ) {
                val restoredIndex =
                    originalQueue
                        .indexOfFirst {
                                candidate ->
                            isSameMediaItem(
                                candidate,
                                currentItem,
                            )
                        }

                if (restoredIndex >= 0) {
                    _state.value =
                        currentState.copy(
                            items =
                                originalQueue,
                            currentIndex =
                                restoredIndex,
                            shuffleEnabled =
                                false,
                        )

                    return
                }
            }

            _state.value =
                currentState.copy(
                    shuffleEnabled = false,
                )

            return
        }

        queueBeforeShuffle =
            currentState.items.toList()

        val selectedIndex =
            currentState.currentIndex
                .coerceIn(
                    currentState
                        .items
                        .indices,
                )

        val selectedItem =
            currentState.items[
                selectedIndex
            ]

        val remainingItems =
            currentState.items
                .toMutableList()
                .apply {
                    removeAt(selectedIndex)
                    shuffle(random)
                }

        _state.value =
            currentState.copy(
                items =
                    listOf(selectedItem) +
                            remainingItems,
                currentIndex = 0,
                shuffleEnabled = true,
            )
    }

    @Synchronized
    fun setRepeat(
        repeatMode: RepeatMode,
    ) {
        _state.value =
            _state.value.copy(
                repeatMode = repeatMode,
            )
    }

    private fun invalidateShuffleRestore(
        state: QueueState,
    ) {
        if (state.shuffleEnabled) {
            queueBeforeShuffle = null
        }
    }

    private fun isSameMediaItem(
        first: MediaItem,
        second: MediaItem,
    ): Boolean {
        return first.id == second.id ||
                first.uri == second.uri
    }
}