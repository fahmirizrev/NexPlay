package com.nexplay.app.playback

import android.content.Context
import androidx.media3.common.Player
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.queue.PlaybackContext
import com.nexplay.app.domain.queue.QueueCompletionAction
import com.nexplay.app.domain.queue.QueueEngine
import com.nexplay.app.domain.queue.QueueState
import com.nexplay.app.domain.queue.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QueuePlaybackCoordinator(
    context: Context,
) {
    private val queueEngine =
        QueueEngine()

    private val playbackEngine =
        PlaybackEngine(
            context.applicationContext,
        )

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main.immediate,
        )

    private var isReleased = false

    private var lastPlaybackStatus =
        PlaybackStatus.IDLE

    val queueState: StateFlow<QueueState> =
        queueEngine.state

    val playbackState: StateFlow<PlaybackState> =
        playbackEngine.state

    internal val mediaSessionDelegate:
            Player
        get() =
            playbackEngine
                .mediaSessionDelegate

    internal val videoViewDelegate:
            Player
        get() =
            playbackEngine
                .mediaSessionDelegate

    init {
        scope.launch {
            playbackEngine.state.collect {
                    playbackState ->
                val previousStatus =
                    lastPlaybackStatus

                lastPlaybackStatus =
                    playbackState.status

                if (
                    playbackState.status ==
                    PlaybackStatus.ENDED &&
                    previousStatus !=
                    PlaybackStatus.ENDED &&
                    isCurrentQueueItem(
                        playbackState.currentMedia,
                    )
                ) {
                    handlePlaybackEnded()
                }
            }
        }
    }

    /**
     * Replaces the entire active queue with a snapshot of the selected playback source.
     * Direct media selection must use this path. Cross-source mixing is reserved for
     * explicit queue actions such as append() and insertNext().
     */
    fun setQueue(
        items: List<MediaItem>,
        currentIndex: Int,
        playbackContext: PlaybackContext,
    ) {
        if (isReleased) {
            return
        }

        queueEngine.setQueue(
            items = items,
            currentIndex = currentIndex,
            playbackContext = playbackContext,
        )

        syncCurrentItem(
            forceRestart = true,
        )
    }

    fun playAt(
        index: Int,
    ) {
        if (isReleased) {
            return
        }

        val previousIndex =
            queueEngine.state.value.currentIndex

        queueEngine.playAt(
            index,
        )

        syncCurrentItem(
            forceRestart =
                queueEngine.state.value.currentIndex !=
                        previousIndex,
        )
    }

    fun next() {
        if (isReleased) {
            return
        }

        val previousIndex =
            queueEngine.state.value.currentIndex

        queueEngine.next()

        syncCurrentItem(
            forceRestart =
                queueEngine.state.value.currentIndex !=
                        previousIndex,
        )
    }

    fun previous() {
        if (isReleased) {
            return
        }

        val previousIndex =
            queueEngine.state.value.currentIndex

        queueEngine.previous()

        syncCurrentItem(
            forceRestart =
                queueEngine.state.value.currentIndex !=
                        previousIndex,
        )
    }

    fun append(
        item: MediaItem,
    ) {
        if (isReleased) {
            return
        }

        queueEngine.append(
            item,
        )

        syncCurrentItem()
    }

    fun insertNext(
        item: MediaItem,
    ) {
        if (isReleased) {
            return
        }

        queueEngine.insertNext(
            item,
        )

        syncCurrentItem()
    }

    fun remove(
        index: Int,
    ) {
        if (isReleased) {
            return
        }

        val previousIndex =
            queueEngine.state.value.currentIndex

        queueEngine.remove(
            index,
        )

        syncCurrentItem(
            forceRestart =
                index == previousIndex,
        )
    }

    fun move(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (isReleased) {
            return
        }

        queueEngine.move(
            fromIndex = fromIndex,
            toIndex = toIndex,
        )

        syncCurrentItem()
    }

    fun clear() {
        if (isReleased) {
            return
        }

        playbackEngine.stop()
        queueEngine.clear()
    }

    fun stopPlayback() {
        if (isReleased) {
            return
        }

        playbackEngine.stop()
    }

    fun toggleShuffle() {
        if (isReleased) {
            return
        }

        queueEngine.toggleShuffle()
        syncCurrentItem()
    }

    fun setRepeat(
        repeatMode: RepeatMode,
    ) {
        if (isReleased) {
            return
        }

        queueEngine.setRepeat(
            repeatMode,
        )
    }

    fun play() {
        if (isReleased) {
            return
        }

        if (
            playbackEngine
                .state
                .value
                .currentMedia == null
        ) {
            syncCurrentItem(
                forceRestart = true,
            )
            return
        }

        playbackEngine.play()
    }

    fun pause() {
        if (isReleased) {
            return
        }

        playbackEngine.pause()
    }

    fun seek(
        positionMs: Long,
    ) {
        if (isReleased) {
            return
        }

        playbackEngine.seek(
            positionMs,
        )
    }

    fun toggleAbRepeatMarker() {
        if (isReleased) {
            return
        }

        playbackEngine.toggleAbRepeatMarker()
    }

    fun release() {
        if (isReleased) {
            return
        }

        isReleased = true
        scope.cancel()
        playbackEngine.release()
    }

    private fun handlePlaybackEnded() {
        if (isReleased) {
            return
        }

        when (
            queueEngine
                .advanceAfterCompletion()
        ) {
            QueueCompletionAction.STOP ->
                Unit

            QueueCompletionAction.REPLAY_CURRENT ->
                playbackEngine.play()

            QueueCompletionAction.ADVANCED ->
                syncCurrentItem(
                    forceRestart = true,
                )
        }
    }

    private fun syncCurrentItem(
        forceRestart: Boolean = false,
    ) {
        if (isReleased) {
            return
        }

        val queueMedia =
            queueEngine
                .state
                .value
                .currentItem

        val playbackMedia =
            playbackEngine
                .state
                .value
                .currentMedia

        if (queueMedia == null) {
            if (playbackMedia != null) {
                playbackEngine.stop()
            }

            return
        }

        if (
            !forceRestart &&
            playbackMedia != null &&
            isSameMediaItem(
                queueMedia,
                playbackMedia,
            )
        ) {
            return
        }

        playbackEngine.prepare(
            queueMedia,
        )

        if (
            playbackEngine
                .state
                .value
                .status !=
            PlaybackStatus.ERROR
        ) {
            playbackEngine.play()
        }
    }

    private fun isCurrentQueueItem(
        media: MediaItem?,
    ): Boolean {
        val currentQueueItem =
            queueEngine
                .state
                .value
                .currentItem

        if (
            media == null ||
            currentQueueItem == null
        ) {
            return false
        }

        return isSameMediaItem(
            media,
            currentQueueItem,
        )
    }

    private fun isSameMediaItem(
        first: MediaItem,
        second: MediaItem,
    ): Boolean {
        return first.id == second.id ||
                first.uri == second.uri
    }
}