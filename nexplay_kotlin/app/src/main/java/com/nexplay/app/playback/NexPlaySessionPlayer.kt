package com.nexplay.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.queue.RepeatMode as QueueRepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
internal class NexPlaySessionPlayer(
    private val coordinator:
    QueuePlaybackCoordinator,
) : ForwardingSimpleBasePlayer(
    coordinator.mediaSessionDelegate,
) {
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main.immediate,
        )

    init {
        scope.launch {
            coordinator
                .queueState
                .collect {
                    invalidateState()
                }
        }
    }

    override fun getState():
            SimpleBasePlayer.State {
        val forwardedState =
            super.getState()

        val queueState =
            coordinator
                .queueState
                .value

        val playlist =
            queueState
                .items
                .map { media ->
                    media.toSessionMediaItemData()
                }

        val currentIndex =
            queueState
                .currentIndex
                .takeIf { index ->
                    index in
                            queueState
                                .items
                                .indices
                }
                ?: C.INDEX_UNSET

        val commands =
            forwardedState
                .availableCommands
                .buildUpon()
                .remove(
                    Player.COMMAND_SET_MEDIA_ITEM,
                )
                .remove(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                )
                .remove(
                    Player.COMMAND_SET_PLAYLIST_METADATA,
                )
                .remove(
                    Player.COMMAND_SET_SPEED_AND_PITCH,
                )
                .remove(
                    Player.COMMAND_STOP,
                )
                .add(
                    Player.COMMAND_SET_REPEAT_MODE,
                )
                .add(
                    Player.COMMAND_SET_SHUFFLE_MODE,
                )
                .add(
                    Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                )
                .apply {
                    if (queueState.items.isNotEmpty()) {
                        add(
                            Player.COMMAND_SEEK_TO_PREVIOUS,
                        )
                        add(
                            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        )
                        add(
                            Player.COMMAND_SEEK_TO_NEXT,
                        )
                        add(
                            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        )
                    } else {
                        remove(
                            Player.COMMAND_SEEK_TO_PREVIOUS,
                        )
                        remove(
                            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        )
                        remove(
                            Player.COMMAND_SEEK_TO_NEXT,
                        )
                        remove(
                            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        )
                    }
                }
                .build()

        return forwardedState
            .buildUpon()
            .setAvailableCommands(
                commands,
            )
            .setPlaylist(
                playlist,
            )
            .setCurrentMediaItemIndex(
                currentIndex,
            )
            .setRepeatMode(
                queueState
                    .repeatMode
                    .toPlayerRepeatMode(),
            )
            .setShuffleModeEnabled(
                queueState.shuffleEnabled,
            )
            .build()
    }

    override fun handleSetPlayWhenReady(
        playWhenReady: Boolean,
    ): ListenableFuture<*> {
        if (playWhenReady) {
            coordinator.play()
        } else {
            coordinator.pause()
        }

        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                coordinator.next()
            }

            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                coordinator.previous()
            }

            Player.COMMAND_SEEK_TO_MEDIA_ITEM -> {
                val queueState =
                    coordinator
                        .queueState
                        .value

                if (
                    mediaItemIndex in
                    queueState.items.indices
                ) {
                    coordinator.playAt(
                        mediaItemIndex,
                    )

                    if (
                        positionMs !=
                        C.TIME_UNSET
                    ) {
                        coordinator.seek(
                            positionMs,
                        )
                    }
                }
            }

            Player.COMMAND_SEEK_TO_DEFAULT_POSITION -> {
                coordinator.seek(
                    0L,
                )
            }

            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> {
                coordinator.seek(
                    if (
                        positionMs ==
                        C.TIME_UNSET
                    ) {
                        0L
                    } else {
                        positionMs
                    },
                )
            }

            else -> {
                return super.handleSeek(
                    mediaItemIndex,
                    positionMs,
                    seekCommand,
                )
            }
        }

        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(
        repeatMode: Int,
    ): ListenableFuture<*> {
        coordinator.setRepeat(
            when (repeatMode) {
                Player.REPEAT_MODE_ONE ->
                    QueueRepeatMode.ONE

                Player.REPEAT_MODE_ALL ->
                    QueueRepeatMode.ALL

                else ->
                    QueueRepeatMode.OFF
            },
        )

        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(
        shuffleModeEnabled: Boolean,
    ): ListenableFuture<*> {
        if (
            coordinator
                .queueState
                .value
                .shuffleEnabled !=
            shuffleModeEnabled
        ) {
            coordinator.toggleShuffle()
        }

        return Futures.immediateVoidFuture()
    }

    override fun handleRelease():
            ListenableFuture<*> {
        scope.cancel()

        return Futures.immediateVoidFuture()
    }

    private fun MediaItem.toSessionMediaItemData():
            SimpleBasePlayer.MediaItemData {
        val metadata =
            toSessionMediaMetadata()

        val mediaItem =
            Media3MediaItem.Builder()
                .setMediaId(
                    id,
                )
                .setMediaMetadata(
                    metadata,
                )
                .build()

        val builder =
            SimpleBasePlayer
                .MediaItemData
                .Builder(
                    "${mediaType.name}:$id:$uri",
                )
                .setMediaItem(
                    mediaItem,
                )
                .setMediaMetadata(
                    metadata,
                )
                .setIsSeekable(
                    true,
                )

        durationMs
            ?.takeIf { duration ->
                duration >= 0L
            }
            ?.let { duration ->
                builder.setDurationUs(
                    duration * 1_000L,
                )
            }

        return builder.build()
    }

    private fun MediaItem.toSessionMediaMetadata():
            MediaMetadata {
        val resolvedTitle =
            title.cleanOrNull()
                ?: displayName.cleanOrNull()
                ?: "Unknown title"

        val builder =
            MediaMetadata.Builder()
                .setTitle(
                    resolvedTitle,
                )
                .setIsPlayable(
                    true,
                )

        durationMs
            ?.takeIf { duration ->
                duration >= 0L
            }
            ?.let { duration ->
                builder.setDurationMs(
                    duration,
                )
            }

        if (this is AudioItem) {
            artist
                .cleanOrNull()
                ?.let { artist ->
                    builder.setArtist(
                        artist,
                    )
                }

            album
                .cleanOrNull()
                ?.let { album ->
                    builder.setAlbumTitle(
                        album,
                    )
                }

            track
                ?.takeIf { track ->
                    track > 0
                }
                ?.let { track ->
                    builder.setTrackNumber(
                        track,
                    )
                }
        }

        return builder.build()
    }

    private fun String?.cleanOrNull():
            String? {
        return this
            ?.trim()
            ?.takeIf { value ->
                value.isNotEmpty() &&
                        value !=
                        "<unknown>"
            }
    }

    private fun QueueRepeatMode.toPlayerRepeatMode():
            Int {
        return when (this) {
            QueueRepeatMode.OFF ->
                Player.REPEAT_MODE_OFF

            QueueRepeatMode.ONE ->
                Player.REPEAT_MODE_ONE

            QueueRepeatMode.ALL ->
                Player.REPEAT_MODE_ALL
        }
    }
}