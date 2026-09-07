package com.nexplay.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexplay.app.data.playlist.PlaylistRepository
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.domain.queue.QueueState
import com.nexplay.app.domain.queue.RepeatMode
import com.nexplay.app.playback.PlaybackState
import com.nexplay.app.playback.PlaybackStatus
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun AudioPlayerScreen(
    queueState: QueueState,
    playbackState: PlaybackState,
    playlistRepository:
        PlaylistRepository,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAbRepeat: () -> Unit,
    onOpenQueue: () -> Unit,
    onSwitchToVideo: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val item =
        queueState.currentItem

    if (item == null) {
        Box(
            modifier =
                modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center,
        ) {
            NexPlayEmptyState(
                icon = Icons.Filled.MusicNote,
                title = "No audio selected",
                subtitle =
                    "Choose an audio file from a folder.",
            )
        }

        return
    }

    val context =
        LocalContext.current

    var isMoreMenuOpen by
        remember {
            mutableStateOf(false)
        }

    var isAddToPlaylistOpen by
        remember {
            mutableStateOf(false)
        }

    var isDetailsDialogOpen by
        remember {
            mutableStateOf(false)
        }

    var dragPositionMs by
    remember(item.uri) {
        mutableStateOf<Float?>(null)
    }

    val durationMs =
        (
                playbackState.durationMs
                    ?: item.durationMs
                    ?: 0L
                )
            .coerceAtLeast(0L)

    val runtimePositionMs =
        playbackState.positionMs
            .coerceIn(
                0L,
                durationMs
                    .takeIf { it > 0L }
                    ?: playbackState
                        .positionMs
                        .coerceAtLeast(0L),
            )

    val displayedPositionMs =
        dragPositionMs
            ?.toLong()
            ?: runtimePositionMs

    val isBusy =
        playbackState.status ==
                PlaybackStatus.PREPARING ||
                playbackState.status ==
                PlaybackStatus.BUFFERING

    val canPrevious =
        queueState.hasPrevious ||
                (
                        queueState.repeatMode ==
                                RepeatMode.ALL &&
                                queueState.items.size > 1
                        )

    val canNext =
        queueState.hasNext ||
                (
                        queueState.repeatMode ==
                                RepeatMode.ALL &&
                                queueState.items.size > 1
                        )

    val density =
        LocalDensity.current

    val minimizeThresholdPx =
        with(density) {
            72.dp.toPx()
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(
                    onBack,
                    onOpenQueue,
                    minimizeThresholdPx,
                ) {
                    var dragDistanceX = 0f
                    var dragDistanceY = 0f

                    detectDragGestures(
                        onDragStart = {
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDragCancel = {
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDragEnd = {
                            when {
                                dragDistanceY >=
                                    minimizeThresholdPx &&
                                    abs(dragDistanceY) >
                                    abs(dragDistanceX) -> {
                                    onBack()
                                }

                                dragDistanceX <=
                                    -minimizeThresholdPx &&
                                    abs(dragDistanceX) >
                                    abs(dragDistanceY) -> {
                                    onOpenQueue()
                                }
                            }

                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDrag = {
                                _,
                                dragAmount ->
                            dragDistanceX +=
                                dragAmount.x
                            dragDistanceY +=
                                dragAmount.y
                        },
                    )
                },
    ) {
        AudioPlayerHeader(
            isMoreMenuOpen =
                isMoreMenuOpen,
            onBack = onBack,
            onSwitchToVideo =
                onSwitchToVideo,
            onToggleMore = {
                isMoreMenuOpen =
                    !isMoreMenuOpen
            },
            onDismissMore = {
                isMoreMenuOpen = false
            },
            onShare = {
                isMoreMenuOpen = false
                shareMedia(
                    context = context,
                    item = item,
                )
            },
            onOpenDetails = {
                isMoreMenuOpen = false
                isDetailsDialogOpen =
                    true
            },
        )

        AudioPlayerIdentity()

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 12.dp,
                    ),
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment =
                    Alignment.Center,
            ) {
                val artworkSize =
                    minOf(
                        maxWidth * 0.80f,
                        maxHeight - 24.dp,
                        316.dp,
                    )
                        .coerceAtLeast(
                            190.dp,
                        )

                MediaVisualBox(
                    item = item,
                    size =
                        artworkSize,
                    cornerRadius =
                        28.dp,
                    fallback = {
                        AudioArtworkFallback(
                            modifier =
                                Modifier.fillMaxSize(),
                        )
                    },
                    highQualityAudioArtwork =
                        item is AudioItem,
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp,
                    ),
            )

            Text(
                text =
                    playerDisplayTitle(
                        item,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations =
                                Int.MAX_VALUE,
                        ),
                maxLines = 1,
                overflow =
                    TextOverflow.Clip,
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground,
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp,
                    ),
            )

            Text(
                text =
                    when (item) {
                        is AudioItem ->
                            item.artist
                                .cleanOrNull()
                                ?: "Unknown Artist"

                        is VideoItem ->
                            "Audio only"
                    },
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp,
                    ),
            )

            Text(
                text =
                    when (item) {
                        is AudioItem ->
                            item.album
                                .cleanOrNull()
                                ?: "Unknown Album"

                        is VideoItem ->
                            item.displayName
                                .cleanOrNull()
                                ?: "Video"
                    },
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            playbackState.error
                ?.message
                ?.takeIf {
                    playbackState.status ==
                            PlaybackStatus.ERROR
                }
                ?.let { message ->
                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp,
                            ),
                    )

                    Text(
                        text = message,
                        maxLines = 2,
                        overflow =
                            TextOverflow
                                .Ellipsis,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                    )
                }
        }

        AudioProgressControls(
            positionMs =
                displayedPositionMs,
            durationMs = durationMs,
            isPlaying =
                playbackState.isPlaying,
            isBusy = isBusy,
            onSeek = onSeek,
            onDragPositionChanged = {
                    value ->
                dragPositionMs = value
            },
            onDragFinished = {
                dragPositionMs = null
            },
        )

        AudioTransportControls(
            isPlaying =
                playbackState.isPlaying,
            isBusy = isBusy,
            canPrevious = canPrevious,
            canNext = canNext,
            shuffleEnabled =
                queueState.shuffleEnabled,
            repeatMode =
                queueState.repeatMode,
            abRepeatStartMs =
                playbackState.abRepeatStartMs,
            abRepeatEndMs =
                playbackState.abRepeatEndMs,
            positionMs =
                playbackState.positionMs,
            durationMs = durationMs,
            onTogglePlayPause =
                onTogglePlayPause,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onToggleShuffle =
                onToggleShuffle,
            onCycleRepeat =
                onCycleRepeat,
            onToggleAbRepeat =
                onToggleAbRepeat,
            onAddToPlaylist = {
                isAddToPlaylistOpen =
                    true
            },
        )

        AudioPlayerFooter()
    }

    if (isAddToPlaylistOpen) {
        AddToPlaylistSheet(
            playlistRepository =
                playlistRepository,
            mediaItems =
                listOf(
                    item,
                ),
            onDismiss = {
                isAddToPlaylistOpen =
                    false
            },
        )
    }

    if (isDetailsDialogOpen) {
        MediaDetailsDialog(
            item = item,
            onDismiss = {
                isDetailsDialogOpen =
                    false
            },
        )
    }
}

@Composable
private fun AudioPlayerHeader(
    isMoreMenuOpen: Boolean,
    onBack: () -> Unit,
    onSwitchToVideo: (() -> Unit)?,
    onToggleMore: () -> Unit,
    onDismissMore: () -> Unit,
    onShare: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = 4.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        NexPlayHeaderActionButton(
            icon =
                Icons.Filled
                    .KeyboardArrowDown,
            contentDescription =
                "Collapse player",
            onClick = onBack,
        )

        Spacer(
            modifier =
                Modifier.weight(1f),
        )

        if (onSwitchToVideo != null) {
            NexPlayHeaderActionButton(
                icon =
                    Icons.Filled.Videocam,
                contentDescription =
                    "Switch to video",
                onClick =
                    onSwitchToVideo,
            )
        }

        Box {
            NexPlayHeaderActionButton(
                icon =
                    Icons.Filled.MoreVert,
                contentDescription =
                    "More",
                onClick = onToggleMore,
            )

            DropdownMenu(
                expanded =
                    isMoreMenuOpen,
                onDismissRequest =
                    onDismissMore,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                "Rhythm Analyzer",
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                Icons.Filled.GraphicEq,
                            contentDescription =
                                null,
                        )
                    },
                    enabled = false,
                    onClick = {},
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Share",
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                Icons.Filled.Share,
                            contentDescription =
                                null,
                        )
                    },
                    onClick = onShare,
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Details",
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                Icons.Filled.Info,
                            contentDescription =
                                null,
                        )
                    },
                    onClick =
                        onOpenDetails,
                )
            }
        }
    }
}

@Composable
private fun AudioPlayerIdentity() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 6.dp,
                    end = 24.dp,
                    bottom = 2.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "N E X P L A Y",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall
                        .copy(
                            letterSpacing =
                                3.sp,
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp,
                    ),
            )

            Text(
                text = "Play the moment",
                style =
                    MaterialTheme
                        .typography
                        .labelSmall
                        .copy(
                            letterSpacing =
                                2.sp,
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }

        Spacer(
            modifier =
                Modifier.weight(
                    1f,
                ),
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment =
                    Alignment.End,
            ) {
                Text(
                    text = "Music",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )

                Text(
                    text = "Lives",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )

                Text(
                    text = "Local",
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        10.dp,
                    ),
            )

            Surface(
                modifier =
                    Modifier
                        .width(
                            1.dp,
                        )
                        .height(
                            46.dp,
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .outline,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {}
        }
    }
}

@Composable
internal fun AudioMiniPlayer(
    queueState: QueueState,
    playbackState: PlaybackState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item =
        queueState.currentItem
            ?: return

    val isBusy =
        playbackState.status ==
                PlaybackStatus.PREPARING ||
                playbackState.status ==
                PlaybackStatus.BUFFERING

    val errorMessage =
        playbackState.error
            ?.message
            ?.takeIf {
                playbackState.status ==
                        PlaybackStatus.ERROR
            }

    val density =
        LocalDensity.current

    val dismissThresholdPx =
        with(density) {
            72.dp.toPx()
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    64.dp,
                )
                .systemGestureExclusion()
                .pointerInput(
                    onStop,
                    onExpand,
                    dismissThresholdPx,
                ) {
                    var dragDistanceX = 0f
                    var dragDistanceY = 0f

                    detectDragGestures(
                        onDragStart = {
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDragCancel = {
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDragEnd = {
                            when {
                                dragDistanceY <=
                                    -dismissThresholdPx &&
                                    abs(dragDistanceY) >
                                    abs(dragDistanceX) -> {
                                    onExpand()
                                }

                                dragDistanceX >=
                                    dismissThresholdPx &&
                                    abs(dragDistanceX) >
                                    abs(dragDistanceY) -> {
                                    onStop()
                                }
                            }

                            dragDistanceX = 0f
                            dragDistanceY = 0f
                        },
                        onDrag = {
                                _,
                                dragAmount ->
                            dragDistanceX +=
                                dragAmount.x
                            dragDistanceY +=
                                dragAmount.y
                        },
                    )
                }
                .clickable(
                    onClick = onExpand,
                ),
        shape =
            RoundedCornerShape(
                16.dp,
            ),
        color =
            MaterialTheme
                .colorScheme
                .background,
        border =
            BorderStroke(
                width = 2.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(
                            alpha = 0.70f,
                        ),
            ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            MediaVisualBox(
                item = item,
                size = 40.dp,
                cornerRadius =
                    12.dp,
                fallback = {
                    AudioMiniArtworkFallback()
                },
            )

            Spacer(
                modifier =
                    Modifier.width(
                        12.dp,
                    ),
            )

            Column(
                modifier =
                    Modifier.weight(1f),
            ) {
                Text(
                    text =
                        playerDisplayTitle(
                            item,
                        ),
                    modifier =
                        Modifier.basicMarquee(
                            iterations =
                                Int.MAX_VALUE,
                        ),
                    maxLines = 1,
                    overflow =
                        TextOverflow.Clip,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp,
                        ),
                )

                Text(
                    text =
                        errorMessage
                            ?: when (item) {
                                is AudioItem ->
                                    item.artist
                                        .cleanOrNull()
                                        ?: "Unknown Artist"

                                is VideoItem ->
                                    "Audio only"
                            },
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        if (
                            errorMessage == null
                        ) {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        } else {
                            MaterialTheme
                                .colorScheme
                                .error
                        },
                )
            }

            IconButton(
                onClick =
                    onTogglePlayPause,
                enabled = !isBusy,
                modifier =
                    Modifier.size(
                        40.dp,
                    ),
            ) {
                Icon(
                    imageVector =
                        if (
                            playbackState
                                .isPlaying
                        ) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled
                                .PlayArrow
                        },
                    contentDescription =
                        if (
                            playbackState
                                .isPlaying
                        ) {
                            "Pause"
                        } else {
                            "Play"
                        },
                    tint =
                        if (isBusy) {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                                .copy(
                                    alpha = 0.38f,
                                )
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                )
            }
        }
    }
}

@Composable
private fun AudioMiniArtworkFallback() {
    Surface(
        modifier =
            Modifier.size(
                40.dp,
            ),
        shape =
            RoundedCornerShape(
                12.dp,
            ),
        color =
            MaterialTheme
                .colorScheme
                .primary
                .copy(
                    alpha = 0.14f,
                ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(
                            alpha = 0.28f,
                        ),
            ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            contentAlignment =
                Alignment.Center,
        ) {
            Text(
                text = "N",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
                        .copy(
                            fontWeight =
                                FontWeight.ExtraBold,
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
            )
        }
    }
}

@Composable
private fun AudioArtworkFallback(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                28.dp,
            ),
        color =
            MaterialTheme
                .colorScheme
                .primary
                .copy(
                    alpha = 0.16f,
                ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center,
        ) {
            Text(
                text = "N E X P L A Y",
                style =
                    MaterialTheme
                        .typography
                        .labelLarge
                        .copy(
                            fontWeight =
                                FontWeight.Medium,
                            letterSpacing =
                                3.sp,
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp,
                    ),
            )

            NexPlayArtworkSignal(
                modifier =
                    Modifier
                        .width(
                            104.dp,
                        )
                        .height(
                            20.dp,
                        ),
            )
        }
    }
}

@Composable
private fun NexPlayArtworkSignal(
    modifier: Modifier = Modifier,
) {
    val signalColor =
        MaterialTheme
            .colorScheme
            .primary

    Canvas(
        modifier = modifier,
    ) {
        if (
            size.width <= 0f ||
            size.height <= 0f
        ) {
            return@Canvas
        }

        val centerY =
            size.height / 2f

        val amplitude =
            3.dp.toPx()

        val waveLength =
            32.dp.toPx()
                .coerceAtLeast(
                    1f,
                )

        val waveFrequency =
            (
                (
                    2.0 *
                        PI
                ) /
                    waveLength
            )
                .toFloat()

        val path =
            Path()

        fun waveY(
            x: Float,
        ): Float {
            return centerY +
                    (
                        amplitude *
                            sin(
                                (
                                    waveFrequency *
                                        x
                                )
                                    .toDouble(),
                            )
                                .toFloat()
                    )
        }

        var x = 0f

        path.moveTo(
            0f,
            waveY(0f),
        )

        val step =
            2.dp.toPx()
                .coerceAtLeast(
                    1f,
                )

        while (
            x + step <
            size.width
        ) {
            x += step

            path.lineTo(
                x,
                waveY(x),
            )
        }

        path.lineTo(
            size.width,
            waveY(
                size.width,
            ),
        )

        drawPath(
            path = path,
            color = signalColor,
            style =
                Stroke(
                    width =
                        2.5.dp.toPx(),
                    cap =
                        StrokeCap.Round,
                ),
        )
    }
}

@Composable
private fun AudioProgressControls(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBusy: Boolean,
    onSeek: (Long) -> Unit,
    onDragPositionChanged:
        (Float?) -> Unit,
    onDragFinished: () -> Unit,
) {
    var localDragValue by
        remember {
            mutableStateOf<Float?>(
                null,
            )
        }

    val hasDuration =
        durationMs > 0L

    val sliderValue =
        if (hasDuration) {
            positionMs
                .coerceIn(
                    0L,
                    durationMs,
                )
                .toFloat()
        } else {
            0f
        }

    val displayedSliderValue =
        localDragValue
            ?: sliderValue

    val isProgressEnabled =
        hasDuration &&
                !isBusy

    val directProgressFraction =
        if (hasDuration) {
            (
                displayedSliderValue /
                    durationMs.toFloat()
            )
                .coerceIn(
                    0f,
                    1f,
                )
        } else {
            0f
        }

    val smoothTargetSliderValue =
        if (
            isPlaying &&
            isProgressEnabled &&
            localDragValue == null
        ) {
            (
                sliderValue +
                    ProgressInterpolationMillis
            )
                .coerceAtMost(
                    durationMs.toFloat(),
                )
        } else {
            displayedSliderValue
        }

    val smoothTargetFraction =
        if (hasDuration) {
            (
                smoothTargetSliderValue /
                    durationMs.toFloat()
            )
                .coerceIn(
                    0f,
                    1f,
                )
        } else {
            0f
        }

    val animatedProgressFraction by
        animateFloatAsState(
            targetValue =
                smoothTargetFraction,
            animationSpec =
                if (
                    isPlaying &&
                    isProgressEnabled &&
                    localDragValue == null
                ) {
                    tween(
                        durationMillis =
                            ProgressInterpolationMillis
                                .toInt(),
                        easing =
                            LinearEasing,
                    )
                } else {
                    snap()
                },
            label =
                "audio-progress-fraction",
        )

    val progressFraction =
        if (localDragValue != null) {
            directProgressFraction
        } else {
            animatedProgressFraction
        }

    val wavePhase =
        remember {
            Animatable(0f)
        }

    val waveAmplitudeScale by
        animateFloatAsState(
            targetValue =
                if (
                    isPlaying &&
                    isProgressEnabled
                ) {
                    1f
                } else {
                    0f
                },
            animationSpec =
                tween(
                    durationMillis = 220,
                    easing =
                        FastOutSlowInEasing,
                ),
            label =
                "audio-wave-amplitude",
        )

    LaunchedEffect(
        isPlaying,
        isProgressEnabled,
    ) {
        if (
            !isPlaying ||
            !isProgressEnabled
        ) {
            return@LaunchedEffect
        }

        while (true) {
            val fullTurn =
                (
                    2.0 *
                        PI
                    )
                    .toFloat()

            val normalizedPhase =
                (
                    (
                        wavePhase.value %
                            fullTurn
                        ) +
                        fullTurn
                    ) %
                    fullTurn

            wavePhase.snapTo(
                normalizedPhase,
            )

            wavePhase.animateTo(
                targetValue =
                    normalizedPhase +
                            fullTurn,
                animationSpec =
                    tween(
                        durationMillis =
                            WaveTravelDurationMillis,
                        easing =
                            LinearEasing,
                    ),
            )
        }
    }

    val activeColor =
        MaterialTheme
            .colorScheme
            .primary

    val inactiveColor =
        MaterialTheme
            .colorScheme
            .surfaceVariant

    val disabledColor =
        MaterialTheme
            .colorScheme
            .onSurfaceVariant
            .copy(
                alpha = 0.38f,
            )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        32.dp,
                    ),
        ) {
            Canvas(
                modifier =
                    Modifier.fillMaxSize(),
            ) {
                val centerY =
                    size.height / 2f

                val activeWidth =
                    size.width *
                            progressFraction

                val resolvedActiveColor =
                    if (isProgressEnabled) {
                        activeColor
                    } else {
                        disabledColor
                    }

                val resolvedInactiveColor =
                    if (isProgressEnabled) {
                        inactiveColor
                    } else {
                        disabledColor
                    }

                val progressStrokeWidth =
                    5.dp.toPx()

                val waveAmplitude =
                    3.5.dp.toPx() *
                            waveAmplitudeScale

                val waveLength =
                    36.dp.toPx()
                        .coerceAtLeast(
                            1f,
                        )

                val waveFrequency =
                    (
                        (
                            2.0 *
                                PI
                            ) /
                            waveLength
                        )
                        .toFloat()

                val phase =
                    wavePhase.value

                val isWavePlaying =
                    isPlaying &&
                            isProgressEnabled

                if (!isWavePlaying) {
                    drawLine(
                        color =
                            resolvedInactiveColor,
                        start =
                            Offset(
                                0f,
                                centerY,
                            ),
                        end =
                            Offset(
                                size.width,
                                centerY,
                            ),
                        strokeWidth =
                            progressStrokeWidth,
                        cap =
                            StrokeCap.Round,
                    )
                } else if (
                    activeWidth <
                    size.width
                ) {
                    drawLine(
                        color =
                            resolvedInactiveColor,
                        start =
                            Offset(
                                activeWidth,
                                centerY,
                            ),
                        end =
                            Offset(
                                size.width,
                                centerY,
                            ),
                        strokeWidth =
                            progressStrokeWidth,
                        cap =
                            StrokeCap.Round,
                    )
                }

                if (activeWidth > 0f) {
                    if (isWavePlaying) {
                        val path =
                            Path()

                        fun waveY(
                            x: Float,
                        ): Float {
                            return centerY +
                                    (
                                        waveAmplitude *
                                            sin(
                                                (
                                                    waveFrequency *
                                                        x +
                                                        phase
                                                    )
                                                    .toDouble(),
                                            )
                                                .toFloat()
                                        )
                        }

                        var x = 0f

                        path.moveTo(
                            0f,
                            waveY(0f),
                        )

                        val step =
                            2.dp.toPx()
                                .coerceAtLeast(
                                    1f,
                                )

                        while (
                            x + step <
                            activeWidth
                        ) {
                            x += step

                            path.lineTo(
                                x,
                                waveY(x),
                            )
                        }

                        path.lineTo(
                            activeWidth,
                            waveY(
                                activeWidth,
                            ),
                        )

                        drawPath(
                            path = path,
                            color =
                                resolvedActiveColor,
                            style =
                                Stroke(
                                    width =
                                        progressStrokeWidth,
                                    cap =
                                        StrokeCap.Round,
                                ),
                        )
                    } else {
                        drawLine(
                            color =
                                resolvedActiveColor,
                            start =
                                Offset(
                                    0f,
                                    centerY,
                                ),
                            end =
                                Offset(
                                    activeWidth,
                                    centerY,
                                ),
                            strokeWidth =
                                progressStrokeWidth,
                            cap =
                                StrokeCap.Round,
                        )
                    }
                }

                if (
                    hasDuration &&
                    size.width > 0f
                ) {
                    val thumbRadius =
                        6.dp.toPx()

                    val thumbX =
                        activeWidth
                            .coerceIn(
                                thumbRadius,
                                (
                                    size.width -
                                        thumbRadius
                                    )
                                    .coerceAtLeast(
                                        thumbRadius,
                                    ),
                            )

                    val thumbY =
                        if (isWavePlaying) {
                            centerY +
                                    (
                                        waveAmplitude *
                                            sin(
                                                (
                                                    waveFrequency *
                                                        thumbX +
                                                        phase
                                                    )
                                                    .toDouble(),
                                            )
                                                .toFloat()
                                        )
                        } else {
                            centerY
                        }

                    drawCircle(
                        color =
                            resolvedActiveColor,
                        radius =
                            thumbRadius,
                        center =
                            Offset(
                                thumbX,
                                thumbY,
                            ),
                    )
                }
            }

            Slider(
                value =
                    displayedSliderValue,
                onValueChange = { value ->
                    localDragValue = value
                    onDragPositionChanged(
                        value,
                    )
                },
                onValueChangeFinished = {
                    val target =
                        localDragValue

                    if (
                        hasDuration &&
                        target != null
                    ) {
                        onSeek(
                            target
                                .toLong()
                                .coerceIn(
                                    0L,
                                    durationMs,
                                ),
                        )
                    }

                    localDragValue = null
                    onDragFinished()
                },
                valueRange =
                    0f..
                            if (hasDuration) {
                                durationMs
                                    .toFloat()
                            } else {
                                1f
                            },
                enabled =
                    isProgressEnabled,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(
                            0f,
                        ),
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    12.dp,
                ),
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement
                    .SpaceBetween,
        ) {
            Text(
                text =
                    formatPlaybackTime(
                        positionMs,
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            Text(
                text =
                    formatPlaybackTime(
                        durationMs,
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }
    }
}

private const val ProgressInterpolationMillis =
    260f

private const val WaveTravelDurationMillis =
    2_400

@Composable
private fun AudioTransportControls(
    isPlaying: Boolean,
    isBusy: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    abRepeatStartMs: Long?,
    abRepeatEndMs: Long?,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAbRepeat: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val repeatIcon =
        if (
            repeatMode ==
            RepeatMode.ONE
        ) {
            Icons.Filled.RepeatOne
        } else {
            Icons.Filled.Repeat
        }

    val repeatDescription =
        when (repeatMode) {
            RepeatMode.OFF ->
                "Repeat off"

            RepeatMode.ONE ->
                "Repeat one"

            RepeatMode.ALL ->
                "Repeat all"
        }

    val abLabel =
        when {
            abRepeatStartMs == null ->
                "A-B"

            abRepeatEndMs == null ->
                "A"

            else ->
                "A-B"
        }

    val abDescription =
        when {
            abRepeatStartMs == null ->
                "Set A marker"

            abRepeatEndMs == null ->
                "Set B marker"

            else ->
                "Clear A-B repeat"
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 12.dp,
                    end = 24.dp,
                    bottom = 12.dp,
                ),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            AudioDeckControlButton(
                icon =
                    Icons.Filled.Replay10,
                contentDescription =
                    "Seek backward 10 seconds",
                caption = "-10s",
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick = {
                    onSeek(
                        (
                                positionMs -
                                        10_000L
                                )
                            .coerceAtLeast(
                                0L,
                            ),
                    )
                },
            )

            AudioDeckControlButton(
                icon =
                    Icons.Filled.Shuffle,
                contentDescription =
                    "Shuffle",
                caption = "Shuffle",
                active =
                    shuffleEnabled,
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick =
                    onToggleShuffle,
            )

            AudioDeckControlButton(
                label = abLabel,
                contentDescription =
                    abDescription,
                caption = "A-B",
                active =
                    abRepeatStartMs != null,
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick =
                    onToggleAbRepeat,
            )

            AudioDeckControlButton(
                icon =
                    Icons.Filled.PlaylistAdd,
                contentDescription =
                    "Add to Playlist",
                caption = "Add",
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick =
                    onAddToPlaylist,
            )

            AudioDeckControlButton(
                icon = repeatIcon,
                contentDescription =
                    repeatDescription,
                caption = "Repeat",
                active =
                    repeatMode !=
                            RepeatMode.OFF,
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick =
                    onCycleRepeat,
            )

            AudioDeckControlButton(
                icon =
                    Icons.Filled.Forward10,
                contentDescription =
                    "Seek forward 10 seconds",
                caption = "+10s",
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick = {
                    val target =
                        positionMs +
                                10_000L

                    onSeek(
                        if (
                            durationMs > 0L
                        ) {
                            target
                                .coerceAtMost(
                                    durationMs,
                                )
                        } else {
                            target
                        },
                    )
                },
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    10.dp,
                ),
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            AudioMainSideButton(
                icon =
                    Icons.Filled
                        .SkipPrevious,
                contentDescription =
                    "Previous",
                enabled =
                    canPrevious &&
                            !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick =
                    onPrevious,
            )

            AudioPrimaryTransportButton(
                isPlaying = isPlaying,
                enabled = !isBusy,
                modifier =
                    Modifier.weight(
                        3f,
                    ),
                onClick =
                    onTogglePlayPause,
            )

            AudioMainSideButton(
                icon =
                    Icons.Filled.SkipNext,
                contentDescription =
                    "Next",
                enabled =
                    canNext &&
                            !isBusy,
                modifier =
                    Modifier.weight(
                        1f,
                    ),
                onClick =
                    onNext,
            )
        }
    }
}

@Composable
private fun AudioDeckControlButton(
    contentDescription: String,
    caption: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon:
        androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String? = null,
    active: Boolean = false,
) {
    val containerColor =
        when {
            !enabled ->
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(
                        alpha = 0.55f,
                    )

            active ->
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(
                        alpha = 0.18f,
                    )

            else ->
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
        }

    val contentColor =
        when {
            !enabled ->
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
                    .copy(
                        alpha = 0.38f,
                    )

            active ->
                MaterialTheme
                    .colorScheme
                    .primary

            else ->
                MaterialTheme
                    .colorScheme
                    .onBackground
        }

    Column(
        modifier = modifier,
        horizontalAlignment =
            Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier =
                Modifier.size(
                    52.dp,
                ),
            shape =
                RoundedCornerShape(
                    26.dp,
                ),
            color = containerColor,
            contentColor =
                contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription =
                            contentDescription,
                        modifier =
                            Modifier.size(
                                22.dp,
                            ),
                    )
                } else {
                    Text(
                        text =
                            label.orEmpty(),
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                                .copy(
                                    fontWeight =
                                        FontWeight.Bold,
                                ),
                        color =
                            contentColor,
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    7.dp,
                ),
        )

        Text(
            text = caption,
            maxLines = 1,
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                if (active) {
                    MaterialTheme
                        .colorScheme
                        .onBackground
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },
        )
    }
}

@Composable
private fun AudioMainSideButton(
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (enabled) {
            MaterialTheme
                .colorScheme
                .onBackground
        } else {
            MaterialTheme
                .colorScheme
                .onSurfaceVariant
                .copy(
                    alpha = 0.38f,
                )
        }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.height(
                84.dp,
            ),
        shape =
            RoundedCornerShape(
                42.dp,
            ),
        color =
            MaterialTheme
                .colorScheme
                .background
                .copy(
                    alpha = 0f,
                ),
        contentColor =
            contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription =
                    contentDescription,
                modifier =
                    Modifier.size(
                        32.dp,
                    ),
            )
        }
    }
}

@Composable
private fun AudioPrimaryTransportButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (enabled) {
            MaterialTheme
                .colorScheme
                .primary
        } else {
            MaterialTheme
                .colorScheme
                .primary
                .copy(
                    alpha = 0.38f,
                )
        }

    val contentColor =
        androidx.compose.ui.graphics.Color.White

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.height(
                60.dp,
            ),
        shape =
            RoundedCornerShape(
                18.dp,
            ),
        color = containerColor,
        contentColor =
            contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                imageVector =
                    if (isPlaying) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled
                            .PlayArrow
                    },
                contentDescription =
                    if (isPlaying) {
                        "Pause"
                    } else {
                        "Play"
                    },
                modifier =
                    Modifier.size(
                        36.dp,
                    ),
            )
        }
    }
}

@Composable
private fun AudioPlayerFooter() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 2.dp,
                    end = 24.dp,
                    bottom = 10.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                Icons.Filled.GraphicEq,
            contentDescription = null,
            modifier =
                Modifier.size(
                    22.dp,
                ),
            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )

        Spacer(
            modifier =
                Modifier.width(
                    10.dp,
                ),
        )

        Text(
            text = "Audio Player",
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )

        Spacer(
            modifier =
                Modifier.width(
                    14.dp,
                ),
        )

        Surface(
            modifier =
                Modifier
                    .weight(
                        1f,
                    )
                    .height(
                        1.dp,
                    ),
            color =
                MaterialTheme
                    .colorScheme
                    .outline,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {}

        Spacer(
            modifier =
                Modifier.width(
                    14.dp,
                ),
        )

        Text(
            text = "/  N E X P L A Y",
            style =
                MaterialTheme
                    .typography
                    .labelSmall
                    .copy(
                        letterSpacing =
                            2.sp,
                    ),
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )
    }
}

private fun playerDisplayTitle(
    item: MediaItem,
): String {
    return when (item) {
        is AudioItem ->
            audioDisplayTitle(
                item,
            )

        is VideoItem ->
            item.title
                .cleanOrNull()
                ?: item.displayName
                    .cleanOrNull()
                    ?.substringBeforeLast(
                        delimiter = ".",
                        missingDelimiterValue =
                            item.displayName,
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                ?: item.id
    }
}

private fun audioDisplayTitle(
    item: AudioItem,
): String {
    val title =
        item.title.cleanOrNull()

    val filename =
        item.displayName
            .cleanOrNull()

    if (
        title != null &&
        filename != null &&
        !title.equals(
            filename,
            ignoreCase = true,
        )
    ) {
        return title
    }

    val fallback =
        filename
            ?.substringBeforeLast(
                delimiter = ".",
                missingDelimiterValue =
                    filename,
            )
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }

    return fallback
        ?: title
        ?: item.id
}

private fun String?.cleanOrNull():
        String? {
    return this
        ?.trim()
        ?.takeIf {
            it.isNotEmpty()
        }
}

private fun formatPlaybackTime(
    milliseconds: Long,
): String {
    val totalSeconds =
        milliseconds
            .coerceAtLeast(0L) /
                1_000L

    val hours =
        totalSeconds /
                3_600L

    val minutes =
        (
                totalSeconds %
                        3_600L
                ) /
                60L

    val seconds =
        totalSeconds %
                60L

    return if (hours > 0L) {
        String.format(
            Locale.US,
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds,
        )
    } else {
        String.format(
            Locale.US,
            "%d:%02d",
            minutes,
            seconds,
        )
    }
}
