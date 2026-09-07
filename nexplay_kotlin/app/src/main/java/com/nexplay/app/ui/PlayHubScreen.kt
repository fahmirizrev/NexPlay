package com.nexplay.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexplay.app.R
import com.nexplay.app.data.playlist.PlaylistRepository
import com.nexplay.app.data.playlist.PlaylistSummary
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.domain.queue.QueueState
import com.nexplay.app.domain.queue.RepeatMode
import com.nexplay.app.playback.PlaybackState
import com.nexplay.app.playback.PlaybackStatus
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun PlayHubScreen(
    queueState: QueueState,
    playbackState: PlaybackState,
    playlistRepository:
        PlaylistRepository,
    onOpenSearch: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenCurrentPlayer: () -> Unit,
    onOpenPlaylist:
        (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlists by
        playlistRepository
            .playlists
            .collectAsState(
                initial = emptyList(),
            )

    var showAboutDialog by
        remember {
            mutableStateOf(false)
        }

    PlayHubRootContent(
        queueState = queueState,
        playbackState = playbackState,
        playlists = playlists,
        onOpenSearch = onOpenSearch,
        onOpenAbout = {
            showAboutDialog = true
        },
        onOpenQueue = onOpenQueue,
        onOpenCurrentPlayer =
            onOpenCurrentPlayer,
        onOpenPlaylist =
            onOpenPlaylist,
        onTogglePlayPause =
            onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        modifier = modifier,
    )

    if (showAboutDialog) {
        NexPlayAboutDialog(
            onDismiss = {
                showAboutDialog = false
            },
        )
    }
}

@Composable
private fun PlayHubRootContent(
    queueState: QueueState,
    playbackState: PlaybackState,
    playlists:
        List<PlaylistSummary>,
    onOpenSearch: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenCurrentPlayer: () -> Unit,
    onOpenPlaylist:
        (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlistListState =
        rememberLazyListState()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        NexPlayAppHeader(
            title = "NexPlay",
            subtitle = "Local-first Media Player",
            isPrimaryScreen = true,
            actions = {
                PlayHubMoreMenu(
                    onOpenSearch = onOpenSearch,
                    onOpenAbout = onOpenAbout,
                )
            },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        top =
                            NexPlaySpacing.contentPaddingTop,
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                NexPlaySpacing.screenPaddingX,
                        ),
            ) {
                PlayHubQueueCard(
                    queueState = queueState,
                    playbackState = playbackState,
                    onOpenQueue = onOpenQueue,
                    onOpenCurrentPlayer =
                        onOpenCurrentPlayer,
                    onTogglePlayPause =
                        onTogglePlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        NexPlaySpacing.sectionGap,
                    ),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal =
                                NexPlaySpacing.screenPaddingX,
                        ),
            ) {
                PlayHubSectionTitleRow(
                    title = "Playlists",
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp,
                    ),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                LazyColumn(
                    state = playlistListState,
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            bottom =
                                NexPlaySpacing.contentPaddingBottom +
                                    NexPlayBottomBarHeight,
                        ),
                ) {
                    if (playlists.isEmpty()) {
                        item(
                            key =
                                "playlists-empty",
                        ) {
                            PlayHubCompactEmptyRow(
                                icon =
                                    Icons.AutoMirrored
                                        .Outlined
                                        .QueueMusic,
                                title =
                                    "No playlists yet",
                                subtitle =
                                    "Create one to organize your media.",
                            )
                        }
                    } else {
                        items(
                            items = playlists,
                            key = {
                                it.id
                            },
                        ) { playlist ->
                            PlayHubPlaylistSummaryRow(
                                playlist =
                                    playlist,
                                onClick = {
                                    onOpenPlaylist(
                                        playlist.id,
                                    )
                                },
                            )
                        }
                    }
                }

                NexPlayScrollbar(
                    listState =
                        playlistListState,
                    modifier =
                        Modifier.align(
                            Alignment.CenterEnd,
                        ),
                )
            }
        }
    }
}

@Composable
private fun PlayHubMoreMenu(
    onOpenSearch: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var expanded by
        remember {
            mutableStateOf(false)
        }

    Box(
        modifier =
            Modifier.size(
                NexPlaySpacing.headerIconButtonSize,
            ),
    ) {
        NexPlayHeaderActionButton(
            icon = Icons.Filled.MoreVert,
            contentDescription = "More menu",
            onClick = {
                expanded = !expanded
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier =
                Modifier
                    .width(
                        208.dp,
                    )
                    .clip(
                        RoundedCornerShape(
                            18.dp,
                        ),
                    ),
            offset =
                DpOffset(
                    x = (-40).dp,
                    y = 0.dp,
                ),
            containerColor =
                MaterialTheme
                    .colorScheme
                    .surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        8.dp,
                    ),
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Search",
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                            Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onOpenSearch()
                },
                modifier =
                    Modifier
                        .height(
                            52.dp,
                        )
                        .clip(
                            RoundedCornerShape(
                                12.dp,
                            ),
                        ),
                contentPadding =
                    PaddingValues(
                        horizontal = 18.dp,
                    ),
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "About NexPlay",
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector =
                            Icons.Outlined.Info,
                        contentDescription =
                            null,
                    )
                },
                onClick = {
                    expanded = false
                    onOpenAbout()
                },
                modifier =
                    Modifier
                        .height(
                            52.dp,
                        )
                        .clip(
                            RoundedCornerShape(
                                12.dp,
                            ),
                        ),
                contentPadding =
                    PaddingValues(
                        horizontal = 18.dp,
                    ),
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp,
                    ),
            )
        }
    }
}

@Composable
private fun PlayHubQueueCard(
    queueState: QueueState,
    playbackState: PlaybackState,
    onOpenQueue: () -> Unit,
    onOpenCurrentPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val item = queueState.currentItem

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    246.dp,
                )
                .clickable(
                    onClick = onOpenQueue,
                ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant,
            ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = 16.dp,
                    top = 14.dp,
                    end = 16.dp,
                    bottom =
                        if (item == null) {
                            18.dp
                        } else {
                            16.dp
                        },
                ),
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier =
                                Modifier.size(
                                    16.dp,
                                ),
                            tint = Color.White,
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            10.dp,
                        ),
                )

                Text(
                    text = "Queue",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                            .copy(
                                fontWeight =
                                    FontWeight.Bold,
                            ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                )
            }

            if (item == null) {
                Spacer(
                    modifier =
                        Modifier.weight(
                            1f,
                        ),
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier =
                            Modifier.size(
                                76.dp,
                            ),
                        shape =
                            RoundedCornerShape(
                                12.dp,
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        Box(
                            contentAlignment =
                                Alignment.Center,
                        ) {
                            Surface(
                                modifier =
                                    Modifier.size(
                                        40.dp,
                                    ),
                                shape = CircleShape,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                            ) {
                                Box(
                                    contentAlignment =
                                        Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled
                                                .PlayArrow,
                                        contentDescription =
                                            null,
                                        modifier =
                                            Modifier.size(
                                                26.dp,
                                            ),
                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,
                                    )
                                }
                            }
                        }
                    }

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
                                "Nothing queued",
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis,
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium
                                    .copy(
                                        fontWeight =
                                            FontWeight.Bold,
                                    ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp,
                                ),
                        )

                        Text(
                            text =
                                "Play something from your library to start a queue.",
                            maxLines = 2,
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
                    }
                }

                Spacer(
                    modifier =
                        Modifier.weight(
                            1f,
                        ),
                )

                return@Column
            }

            val durationMs =
                (
                    playbackState.durationMs
                        ?: item.durationMs
                        ?: 0L
                )
                    .coerceAtLeast(0L)

            val positionMs =
                clampPlayHubPosition(
                    positionMs =
                        playbackState.positionMs,
                    durationMs = durationMs,
                )

            val progress =
                playHubProgressRatio(
                    positionMs = positionMs,
                    durationMs = durationMs,
                )

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

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp,
                    ),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick =
                                onOpenCurrentPlayer,
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                PlayHubMediaPreview(
                    item = item,
                    size = 76.dp,
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
                            playHubDisplayTitle(
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
                                .titleMedium
                                .copy(
                                    fontWeight =
                                        FontWeight.Bold,
                                ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp,
                            ),
                    )

                    Text(
                        text =
                            playHubArtistLabel(
                                item,
                            ),
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

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp,
                            ),
                    )

                    Text(
                        text =
                            playHubAlbumOrSourceLabel(
                                item,
                            ),
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

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp,
                            ),
                    )

                    PlayHubQueueProgressBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        isPlaying =
                            playbackState.isPlaying,
                        isBusy = isBusy,
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp,
                            ),
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                formatPlayHubDuration(
                                    positionMs,
                                ),
                            maxLines = 1,
                            overflow =
                                TextOverflow.Clip,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                        )

                        Spacer(
                            modifier =
                                Modifier.weight(1f),
                        )

                        Text(
                            text =
                                if (durationMs > 0L) {
                                    formatPlayHubDuration(
                                        durationMs,
                                    )
                                } else {
                                    "--"
                                },
                            maxLines = 1,
                            overflow =
                                TextOverflow.Clip,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp,
                    ),
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled =
                        canPrevious &&
                            !isBusy,
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled
                                .SkipPrevious,
                        contentDescription =
                            "Previous",
                        modifier =
                            Modifier.size(
                                24.dp,
                            ),
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            18.dp,
                        ),
                )

                Surface(
                    onClick =
                        onTogglePlayPause,
                    enabled = !isBusy,
                    modifier =
                        Modifier.size(
                            56.dp,
                        ),
                    shape = CircleShape,
                    color =
                        if (isBusy) {
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(
                                    alpha = 0.38f,
                                )
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                    contentColor =
                        Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                if (
                                    playbackState
                                        .isPlaying
                                ) {
                                    Icons.Filled.Pause
                                } else {
                                    Icons.Filled.PlayArrow
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
                            modifier =
                                Modifier.size(
                                    28.dp,
                                ),
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            18.dp,
                        ),
                )

                IconButton(
                    onClick = onNext,
                    enabled =
                        canNext &&
                            !isBusy,
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled
                                .SkipNext,
                        contentDescription =
                            "Next",
                        modifier =
                            Modifier.size(
                                24.dp,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayHubQueueProgressBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isBusy: Boolean,
) {
    val hasDuration =
        durationMs > 0L

    val progressEnabled =
        hasDuration &&
            !isBusy

    val directProgressFraction =
        if (hasDuration) {
            (
                positionMs
                    .coerceIn(
                        0L,
                        durationMs,
                    )
                    .toFloat() /
                    durationMs.toFloat()
            )
                .coerceIn(
                    0f,
                    1f,
                )
        } else {
            0f
        }

    val smoothTargetPositionMs =
        if (
            isPlaying &&
            progressEnabled
        ) {
            (
                positionMs.toFloat() +
                    PlayHubProgressInterpolationMillis
            )
                .coerceAtMost(
                    durationMs.toFloat(),
                )
        } else {
            positionMs.toFloat()
        }

    val smoothTargetFraction =
        if (hasDuration) {
            (
                smoothTargetPositionMs /
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
                    progressEnabled
                ) {
                    tween(
                        durationMillis =
                            PlayHubProgressInterpolationMillis
                                .toInt(),
                        easing =
                            LinearEasing,
                    )
                } else {
                    snap()
                },
            label =
                "playhub-queue-progress",
        )

    val progressFraction =
        if (progressEnabled) {
            animatedProgressFraction
        } else {
            directProgressFraction
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
                    progressEnabled
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
                "playhub-queue-wave-amplitude",
        )

    LaunchedEffect(
        isPlaying,
        progressEnabled,
    ) {
        if (
            !isPlaying ||
            !progressEnabled
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
                            PlayHubWaveTravelDurationMillis,
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

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    18.dp,
                ),
    ) {
        if (
            size.width <= 0f ||
            size.height <= 0f
        ) {
            return@Canvas
        }

        val centerY =
            size.height / 2f

        val activeWidth =
            size.width *
                progressFraction

        val resolvedActiveColor =
            if (progressEnabled) {
                activeColor
            } else {
                disabledColor
            }

        val resolvedInactiveColor =
            if (progressEnabled) {
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
                progressEnabled

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

        if (hasDuration) {
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
}

private const val PlayHubProgressInterpolationMillis =
    260f

private const val PlayHubWaveTravelDurationMillis =
    2_400


@Composable
private fun PlayHubMediaPreview(
    item: MediaItem,
    size: androidx.compose.ui.unit.Dp,
) {
    MediaThumbnailBox(
        item = item,
        size = size,
        fallbackIcon =
            when (item) {
                is AudioItem ->
                    Icons.Filled.MusicNote

                is VideoItem ->
                    Icons.Filled.PlayArrow
            },
        cornerRadius = 12.dp,
    )
}

@Composable
private fun PlayHubSectionTitleRow(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier =
                Modifier.weight(1f),
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            style =
                MaterialTheme
                    .typography
                    .titleSmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )

        if (
            actionLabel != null &&
            onAction != null
        ) {
            Text(
                text = actionLabel,
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                6.dp,
                            ),
                        )
                        .clickable(
                            onClick = onAction,
                        )
                        .padding(
                            vertical = 2.dp,
                        ),
                style =
                    MaterialTheme
                        .typography
                        .titleSmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
            )
        }
    }
}

@Composable
private fun PlayHubCompactEmptyRow(
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        NexPlaySpacing.screenPaddingX,
                    vertical = 7.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Surface(
            modifier =
                Modifier.size(
                    40.dp,
                ),
            shape =
                RoundedCornerShape(
                    8.dp,
                ),
            color =
                MaterialTheme
                    .colorScheme
                    .surfaceVariant,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier =
                        Modifier.size(
                            22.dp,
                        ),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onBackground,
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(
                    10.dp,
                ),
        )

        Column(
            modifier =
                Modifier.weight(1f),
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground,
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp,
                    ),
            )

            Text(
                text = subtitle,
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
        }

        Spacer(
            modifier =
                Modifier.width(
                    8.dp,
                ),
        )

        Box(
            modifier =
                Modifier.size(
                    NexPlaySpacing
                        .trailingIconBoxSize,
                ),
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                imageVector =
                    Icons.Filled
                        .ChevronRight,
                contentDescription = null,
                modifier =
                    Modifier.size(
                        NexPlaySpacing
                            .trailingIconSize,
                    ),
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PlayHubQueueScreen(
    queueState: QueueState,
    onBack: () -> Unit,
    onPlayItem: (
        Int,
        MediaItem,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemCount = queueState.items.size
    val listState =
        rememberLazyListState()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        NexPlayAppHeader(
            title = "Queue",
            subtitle =
                if (itemCount == 1) {
                    "1 item"
                } else {
                    "$itemCount items"
                },
            onBack = onBack,
        )

        Box(
            modifier =
                Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start =
                            NexPlaySpacing.screenPaddingX,
                        top =
                            NexPlaySpacing.contentPaddingTop,
                        end =
                            NexPlaySpacing.screenPaddingX,
                        bottom =
                            NexPlaySpacing.contentPaddingBottom,
                    ),
            ) {
                if (queueState.items.isEmpty()) {
                    item(
                        key = "queue-empty",
                    ) {
                        NexPlayEmptyState(
                            icon =
                                Icons.AutoMirrored.Outlined.QueueMusic,
                            title = "Queue is empty",
                            subtitle =
                                "Play something from your library to start a queue.",
                        )
                    }
                } else {
                    itemsIndexed(
                        items = queueState.items,
                        key = {
                                _,
                                item ->
                            item.uri
                        },
                    ) {
                            index,
                            item ->
                        PlayHubQueueRow(
                            item = item,
                            isCurrent =
                                index ==
                                    queueState.currentIndex,
                            onClick = {
                                onPlayItem(
                                    index,
                                    item,
                                )
                            },
                        )
                    }
                }
            }

            NexPlayScrollbar(
                listState = listState,
                modifier =
                    Modifier.align(
                        Alignment.CenterEnd,
                    ),
            )
        }
    }
}

@Composable
private fun PlayHubQueueRow(
    item: MediaItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        12.dp,
                    ),
                )
                .clickable(
                    onClick = onClick,
                )
                .padding(
                    vertical = 8.dp,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        PlayHubMediaPreview(
            item = item,
            size = 48.dp,
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
                    playHubDisplayTitle(
                        item,
                    ),
                modifier =
                    if (isCurrent) {
                        Modifier.basicMarquee(
                            iterations =
                                Int.MAX_VALUE,
                        )
                    } else {
                        Modifier
                    },
                maxLines = 1,
                overflow =
                    if (isCurrent) {
                        TextOverflow.Clip
                    } else {
                        TextOverflow.Ellipsis
                    },
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
                        .copy(
                            fontWeight =
                                if (isCurrent) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                        ),
                color =
                    if (isCurrent) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onBackground
                    },
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp,
                    ),
            )

            Text(
                text =
                    playHubArtistLabel(
                        item,
                    ),
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

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp,
                    ),
            )

            Text(
                text =
                    playHubAlbumOrSourceLabel(
                        item,
                    ),
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }

        Spacer(
            modifier =
                Modifier.width(
                    12.dp,
                ),
        )

        if (isCurrent) {
            Icon(
                imageVector =
                    Icons.Filled.PlayArrow,
                contentDescription =
                    "Currently playing",
                modifier =
                    Modifier.size(
                        22.dp,
                    ),
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
            )
        } else {
            Text(
                text =
                    item.durationMs
                        ?.takeIf { duration ->
                            duration > 0L
                        }
                        ?.let(
                            ::formatPlayHubDuration,
                        )
                        ?: "--",
                maxLines = 1,
                overflow =
                    TextOverflow.Clip,
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun NexPlayAboutDialog(
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    28.dp,
                ),
            color =
                MaterialTheme
                    .colorScheme
                    .surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        start = 26.dp,
                        top = 26.dp,
                        end = 26.dp,
                        bottom = 18.dp,
                    ),
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Image(
                        painter =
                            painterResource(
                                id =
                                    R.drawable
                                        .nexplay_logo,
                            ),
                        contentDescription =
                            "NexPlay logo",
                        modifier =
                            Modifier
                                .size(
                                    74.dp,
                                )
                                .clip(
                                    RoundedCornerShape(
                                        22.dp,
                                    ),
                                ),
                        contentScale =
                            ContentScale.Crop,
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                20.dp,
                            ),
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f),
                    ) {
                        Text(
                            text = "NexPlay",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall
                                    .copy(
                                        fontWeight =
                                            FontWeight
                                                .ExtraBold,
                                    ),
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp,
                                ),
                        )

                        Text(
                            text =
                                "Local-first Media Player",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            24.dp,
                        ),
                )

                AboutDivider()

                Text(
                    text =
                        "A local-first media player for audio and video.\nNo cloud. No account. Your media stays on your device.",
                    modifier =
                        Modifier.padding(
                            vertical = 18.dp,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                )

                AboutDivider()

                AboutInfoRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    showChevron = true,
                )

                AboutDivider()

                AboutInfoRow(
                    icon =
                        Icons.Outlined.Person,
                    title = "Developed by",
                    subtitle =
                        "FRZDEV\nSoftware Architect • Product Engineer",
                    showChevron = true,
                )

                AboutDivider()

                AboutInfoRow(
                    icon = Icons.Outlined.Info,
                    title = "License",
                    subtitle =
                        "GNU GPL v3.0 only\nCopyright © 2026 FRZDEV",
                )

                AboutDivider()

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp,
                        ),
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text(
                            text = "Close",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon:
        androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = false,
) {
    Row(
        modifier =
            Modifier.padding(
                vertical = 18.dp,
            ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier =
                Modifier.size(
                    28.dp,
                ),
            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
        )

        Spacer(
            modifier =
                Modifier.width(
                    18.dp,
                ),
        )

        Column(
            modifier =
                Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
                        .copy(
                            fontWeight =
                                FontWeight.Bold,
                        ),
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp,
                    ),
            )

            Text(
                text = subtitle,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }

        if (showChevron) {
            Icon(
                imageVector =
                    Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier =
                    Modifier.size(
                        30.dp,
                    ),
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    1.dp,
                ),
        color =
            MaterialTheme
                .colorScheme
                .outlineVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {}
}

internal fun clampPlayHubPosition(
    positionMs: Long,
    durationMs: Long,
): Long {
    if (durationMs <= 0L) {
        return positionMs.coerceAtLeast(0L)
    }

    return positionMs.coerceIn(
        0L,
        durationMs,
    )
}

internal fun playHubProgressRatio(
    positionMs: Long,
    durationMs: Long,
): Float {
    if (durationMs <= 0L) {
        return 0f
    }

    return (
        clampPlayHubPosition(
            positionMs = positionMs,
            durationMs = durationMs,
        ).toFloat() /
            durationMs.toFloat()
    )
        .coerceIn(
            0f,
            1f,
        )
}

private fun playHubDisplayTitle(
    item: MediaItem,
): String {
    return item.title
        .trim()
        .takeIf(String::isNotEmpty)
        ?: item.displayName
            .trim()
            .takeIf(String::isNotEmpty)
            ?.substringBeforeLast(
                delimiter = ".",
                missingDelimiterValue =
                    item.displayName,
            )
        ?: item.id
}

private fun playHubArtistLabel(
    item: MediaItem,
): String {
    return when (item) {
        is AudioItem ->
            item.artist
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: "Unknown Artist"

        is VideoItem ->
            "Video"
    }
}

private fun playHubAlbumOrSourceLabel(
    item: MediaItem,
): String {
    return when (item) {
        is AudioItem ->
            item.album
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: item.folder
                    ?.name
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                ?: "Unknown Album"

        is VideoItem ->
            item.folder
                ?.name
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: "Unknown Folder"
    }
}

private fun formatPlayHubDuration(
    durationMs: Long,
): String {
    val totalSeconds =
        durationMs
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
        "$hours:" +
            minutes
                .toString()
                .padStart(2, '0') +
            ":" +
            seconds
                .toString()
                .padStart(2, '0')
    } else {
        "$minutes:" +
            seconds
                .toString()
                .padStart(2, '0')
    }
}
