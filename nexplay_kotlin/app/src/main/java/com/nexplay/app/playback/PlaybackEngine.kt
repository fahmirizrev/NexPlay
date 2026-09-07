package com.nexplay.app.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nexplay.app.domain.model.MediaItem
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackEngine(
    context: Context,
) : Player.Listener {
    private val player =
        ExoPlayer.Builder(
            context.applicationContext,
        )
            .setAudioAttributes(
                AudioAttributes.DEFAULT,
                true,
            )
            .setHandleAudioBecomingNoisy(
                true,
            )
            .build()
            .also { player ->
                player.addListener(this)
            }

    internal val mediaSessionDelegate:
            Player
        get() = player

    private val positionHandler =
        Handler(
            Looper.getMainLooper(),
        )

    private var isReleased = false

    private val _state =
        MutableStateFlow(
            PlaybackState(),
        )

    val state: StateFlow<PlaybackState> =
        _state.asStateFlow()

    private val positionUpdater =
        object : Runnable {
            override fun run() {
                if (isReleased) {
                    return
                }

                if (!enforceAbRepeatBoundary()) {
                    publishRuntimeState()
                }

                if (player.isPlaying) {
                    positionHandler.postDelayed(
                        this,
                        if (
                            _state.value
                                .isAbRepeatEnabled
                        ) {
                            AbRepeatUpdateIntervalMs
                        } else {
                            PositionUpdateIntervalMs
                        },
                    )
                }
            }
        }

    fun prepare(
        media: MediaItem,
        startPositionMs: Long = 0L,
    ) {
        if (isReleased) {
            return
        }

        val safeStartPosition =
            resolveStartPosition(
                media = media,
                requestedPositionMs = startPositionMs,
            )

        try {
            player.pause()
            player.clearMediaItems()

            val sourceUri =
                resolveSourceUri(
                    media.uri,
                )

            if (sourceUri == null) {
                _state.value =
                    PlaybackState(
                        currentMedia = media,
                        status = PlaybackStatus.ERROR,
                        positionMs = safeStartPosition,
                        durationMs =
                            media.durationMs
                                ?.takeIf { duration ->
                                    duration >= 0L
                                },
                        error =
                            PlaybackError(
                                kind =
                                    PlaybackErrorKind.SOURCE,
                                message =
                                    "This media item has no playable source.",
                            ),
                    )

                stopPositionUpdates()
                return
            }

            val media3Item =
                buildMedia3Item(
                    media = media,
                    sourceUri = sourceUri,
                )

            _state.value =
                PlaybackState(
                    currentMedia = media,
                    status =
                        PlaybackStatus.PREPARING,
                    isPlaying = false,
                    positionMs =
                        safeStartPosition,
                    durationMs =
                        media.durationMs
                            ?.takeIf { duration ->
                                duration >= 0L
                            },
                )

            player.setMediaItem(
                media3Item,
                safeStartPosition,
            )
            player.prepare()
        } catch (error: RuntimeException) {
            publishOperationError(
                media = media,
                message =
                    "This media file could not be prepared.",
                error = error,
            )
        }
    }

    fun play() {
        if (
            isReleased ||
            _state.value.currentMedia == null
        ) {
            return
        }

        try {
            if (
                player.playbackState ==
                Player.STATE_ENDED
            ) {
                player.seekTo(0L)
            }

            player.play()
            publishRuntimeState()
            updatePositionLoop()
        } catch (error: RuntimeException) {
            publishOperationError(
                media =
                    _state.value.currentMedia
                        ?: return,
                message =
                    "Could not start playback.",
                error = error,
            )
        }
    }

    fun pause() {
        if (
            isReleased ||
            _state.value.currentMedia == null
        ) {
            return
        }

        try {
            player.pause()
            publishRuntimeState()
            stopPositionUpdates()
        } catch (error: RuntimeException) {
            publishOperationError(
                media =
                    _state.value.currentMedia
                        ?: return,
                message =
                    "Could not pause playback.",
                error = error,
            )
        }
    }

    fun stop() {
        if (isReleased) {
            return
        }

        try {
            player.stop()
            player.clearMediaItems()
            stopPositionUpdates()

            _state.value =
                PlaybackState()
        } catch (error: RuntimeException) {
            val media =
                _state.value.currentMedia
                    ?: return

            publishOperationError(
                media = media,
                message =
                    "Could not stop playback.",
                error = error,
            )
        }
    }

    fun seek(
        positionMs: Long,
    ) {
        if (
            isReleased ||
            _state.value.currentMedia == null
        ) {
            return
        }

        val targetPosition =
            resolveSeekPosition(
                requestedPositionMs =
                    positionMs,
            )

        try {
            player.seekTo(
                targetPosition,
            )

            _state.value =
                _state.value.copy(
                    positionMs =
                        targetPosition,
                    error = null,
                )
        } catch (error: RuntimeException) {
            publishOperationError(
                media =
                    _state.value.currentMedia
                        ?: return,
                message =
                    "Could not seek this media file.",
                error = error,
            )
        }
    }

    fun toggleAbRepeatMarker() {
        if (
            isReleased ||
            _state.value.currentMedia == null
        ) {
            return
        }

        val currentState =
            _state.value

        val currentPosition =
            resolveSeekPosition(
                currentState.positionMs,
            )

        val nextState =
            when {
                currentState.abRepeatStartMs == null ->
                    currentState.copy(
                        abRepeatStartMs =
                            currentPosition,
                        abRepeatEndMs = null,
                    )

                currentState.abRepeatEndMs == null -> {
                    val start =
                        currentState
                            .abRepeatStartMs

                    if (
                        start == null ||
                        currentPosition <= start
                    ) {
                        currentState.copy(
                            abRepeatStartMs =
                                currentPosition,
                            abRepeatEndMs = null,
                        )
                    } else {
                        currentState.copy(
                            abRepeatEndMs =
                                currentPosition,
                        )
                    }
                }

                else ->
                    currentState.copy(
                        abRepeatStartMs = null,
                        abRepeatEndMs = null,
                    )
            }

        _state.value =
            nextState.copy(
                positionMs =
                    currentPosition,
            )
    }

    fun release() {
        if (isReleased) {
            return
        }

        isReleased = true
        stopPositionUpdates()

        player.removeListener(this)
        player.release()

        _state.value =
            PlaybackState()
    }

    override fun onPlaybackStateChanged(
        playbackState: Int,
    ) {
        if (isReleased) {
            return
        }

        if (
            playbackState ==
            Player.STATE_ENDED &&
            enforceAbRepeatBoundary()
        ) {
            updatePositionLoop()
            return
        }

        publishRuntimeState(
            statusOverride =
                mapPlaybackStatus(
                    playbackState,
                ),
        )

        updatePositionLoop()
    }

    override fun onIsPlayingChanged(
        isPlaying: Boolean,
    ) {
        if (isReleased) {
            return
        }

        publishRuntimeState()
        updatePositionLoop()
    }

    override fun onPlayerError(
        error: PlaybackException,
    ) {
        if (isReleased) {
            return
        }

        val currentState =
            _state.value

        val media =
            currentState.currentMedia
                ?: return

        _state.value =
            currentState.copy(
                currentMedia = media,
                status = PlaybackStatus.ERROR,
                isPlaying = false,
                positionMs =
                    player.currentPosition
                        .coerceAtLeast(0L),
                durationMs =
                    resolveRuntimeDuration(
                        media,
                    ),
                error =
                    PlaybackError(
                        kind =
                            PlaybackErrorKind.RUNTIME,
                        message =
                            "Playback failed for this media file.",
                        diagnosticCode =
                            error.errorCodeName,
                    ),
            )

        stopPositionUpdates()
    }

    private fun enforceAbRepeatBoundary():
            Boolean {
        if (isReleased) {
            return false
        }

        val currentState =
            _state.value

        val start =
            currentState.abRepeatStartMs
                ?: return false

        val end =
            currentState.abRepeatEndMs
                ?: return false

        val currentPosition =
            player.currentPosition
                .coerceAtLeast(0L)

        if (currentPosition < end) {
            return false
        }

        player.seekTo(
            start,
        )

        if (player.playWhenReady) {
            player.play()
        }

        _state.value =
            currentState.copy(
                status =
                    PlaybackStatus.READY,
                isPlaying =
                    currentState.isPlaying,
                positionMs = start,
                error = null,
            )

        return true
    }

    private fun publishRuntimeState(
        statusOverride: PlaybackStatus? = null,
    ) {
        val currentState =
            _state.value

        val media =
            currentState.currentMedia
                ?: return

        val status =
            when {
                player.playerError != null ->
                    PlaybackStatus.ERROR

                statusOverride != null ->
                    statusOverride

                else ->
                    mapPlaybackStatus(
                        player.playbackState,
                    )
            }

        _state.value =
            currentState.copy(
                status = status,
                isPlaying =
                    player.isPlaying &&
                            status !=
                            PlaybackStatus.ERROR,
                positionMs =
                    player.currentPosition
                        .coerceAtLeast(0L),
                durationMs =
                    resolveRuntimeDuration(
                        media,
                    ),
                error =
                    if (
                        status ==
                        PlaybackStatus.ERROR
                    ) {
                        currentState.error
                    } else {
                        null
                    },
            )
    }

    private fun mapPlaybackStatus(
        playbackState: Int,
    ): PlaybackStatus {
        if (player.playerError != null) {
            return PlaybackStatus.ERROR
        }

        return when (playbackState) {
            Player.STATE_IDLE ->
                if (
                    _state.value.status ==
                    PlaybackStatus.PREPARING
                ) {
                    PlaybackStatus.PREPARING
                } else {
                    PlaybackStatus.IDLE
                }

            Player.STATE_BUFFERING ->
                PlaybackStatus.BUFFERING

            Player.STATE_READY ->
                PlaybackStatus.READY

            Player.STATE_ENDED ->
                PlaybackStatus.ENDED

            else ->
                PlaybackStatus.IDLE
        }
    }

    private fun buildMedia3Item(
        media: MediaItem,
        sourceUri: Uri,
    ): Media3MediaItem {
        val builder =
            Media3MediaItem.Builder()
                .setMediaId(
                    media.id,
                )
                .setUri(
                    sourceUri,
                )

        media.mimeType
            ?.trim()
            ?.takeIf { mimeType ->
                mimeType.isNotEmpty()
            }
            ?.let { mimeType ->
                builder.setMimeType(
                    mimeType,
                )
            }

        return builder.build()
    }

    private fun resolveSourceUri(
        rawUri: String,
    ): Uri? {
        val source =
            rawUri.trim()

        if (source.isEmpty()) {
            return null
        }

        val parsedUri =
            Uri.parse(
                source,
            )

        return if (
            parsedUri.scheme
                .isNullOrBlank()
        ) {
            Uri.fromFile(
                File(source),
            )
        } else {
            parsedUri
        }
    }

    private fun resolveStartPosition(
        media: MediaItem,
        requestedPositionMs: Long,
    ): Long {
        val duration =
            media.durationMs
                ?.takeIf { value ->
                    value >= 0L
                }

        return if (duration == null) {
            requestedPositionMs
                .coerceAtLeast(0L)
        } else {
            requestedPositionMs
                .coerceIn(
                    0L,
                    duration,
                )
        }
    }

    private fun resolveSeekPosition(
        requestedPositionMs: Long,
    ): Long {
        val duration =
            _state.value.durationMs

        return if (
            duration == null ||
            duration < 0L
        ) {
            requestedPositionMs
                .coerceAtLeast(0L)
        } else {
            requestedPositionMs
                .coerceIn(
                    0L,
                    duration,
                )
        }
    }

    private fun resolveRuntimeDuration(
        media: MediaItem,
    ): Long? {
        val runtimeDuration =
            player.duration

        if (
            runtimeDuration !=
            C.TIME_UNSET &&
            runtimeDuration >= 0L
        ) {
            return runtimeDuration
        }

        return media.durationMs
            ?.takeIf { duration ->
                duration >= 0L
            }
    }

    private fun publishOperationError(
        media: MediaItem,
        message: String,
        error: RuntimeException,
    ) {
        _state.value =
            _state.value.copy(
                currentMedia = media,
                status = PlaybackStatus.ERROR,
                isPlaying = false,
                error =
                    PlaybackError(
                        kind =
                            PlaybackErrorKind.RUNTIME,
                        message = message,
                        diagnosticCode =
                            error::class.java
                                .simpleName
                                .takeIf { name ->
                                    name.isNotBlank()
                                },
                    ),
            )

        stopPositionUpdates()
    }

    private fun updatePositionLoop() {
        stopPositionUpdates()

        if (
            !isReleased &&
            player.isPlaying
        ) {
            positionHandler.post(
                positionUpdater,
            )
        }
    }

    private fun stopPositionUpdates() {
        positionHandler.removeCallbacks(
            positionUpdater,
        )
    }

    private companion object {
        const val PositionUpdateIntervalMs =
            250L

        const val AbRepeatUpdateIntervalMs =
            50L
    }
}
