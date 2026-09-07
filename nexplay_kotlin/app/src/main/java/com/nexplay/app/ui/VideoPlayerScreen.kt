package com.nexplay.app.ui

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.domain.queue.QueueState
import com.nexplay.app.domain.queue.RepeatMode
import com.nexplay.app.playback.PlaybackState
import com.nexplay.app.playback.PlaybackStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun VideoPlayerScreen(
    queueState: QueueState,
    playbackState: PlaybackState,
    player: Player,
    onBack: () -> Unit,
    onSwitchToAudioOnly: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val activity =
        remember(context) {
            context.findVideoPlayerActivity()
        }

    val childLockPatternStore =
        remember(
            context.applicationContext,
        ) {
            ChildLockPatternPreferenceStore(
                context.applicationContext,
            )
        }

    val hapticFeedback =
        LocalHapticFeedback.current

    val currentVideo =
        queueState.currentItem as? VideoItem
            ?: playbackState.currentMedia
                    as? VideoItem

    var isFullscreen by
    rememberSaveable {
        mutableStateOf(false)
    }

    var pendingSeekPositionMs by
    remember(currentVideo?.id) {
        mutableStateOf<Long?>(null)
    }

    var areControlsVisible by
        rememberSaveable {
            mutableStateOf(true)
        }

    var isControlsLocked by
        rememberSaveable {
            mutableStateOf(false)
        }

    var isMoreMenuOpen by
        remember {
            mutableStateOf(false)
        }

    var isSetChildLockPatternDialogOpen by
        rememberSaveable {
            mutableStateOf(false)
        }

    var isVerifyChildLockPatternDialogOpen by
        rememberSaveable {
            mutableStateOf(false)
        }

    var isDetailsDialogOpen by
        rememberSaveable {
            mutableStateOf(false)
        }

    var controlsAutoHideRequest by
        remember {
            mutableStateOf(0)
        }

    fun activateChildLock() {
        activity?.startLockTask()

        isControlsLocked = true
        areControlsVisible = false
    }

    fun exitChildLock() {
        activity?.stopLockTaskIfActive()

        isControlsLocked = false
        areControlsVisible = true
        controlsAutoHideRequest += 1
    }

    fun openChildLockPatternVerification() {
        isVerifyChildLockPatternDialogOpen =
            true
    }

    fun showControls() {
        if (isControlsLocked) {
            return
        }

        areControlsVisible = true
        controlsAutoHideRequest += 1
    }

    fun toggleControls() {
        if (isControlsLocked) {
            return
        }

        if (areControlsVisible) {
            areControlsVisible = false
        } else {
            showControls()
        }
    }

    LaunchedEffect(
        playbackState.isPlaying,
        currentVideo?.id,
        isFullscreen,
        isControlsLocked,
    ) {
        if (isControlsLocked) {
            areControlsVisible = false
            return@LaunchedEffect
        }

        if (playbackState.isPlaying) {
            showControls()
        } else {
            areControlsVisible = true
        }
    }

    LaunchedEffect(
        playbackState.isPlaying,
        areControlsVisible,
        controlsAutoHideRequest,
        isMoreMenuOpen,
        isControlsLocked,
    ) {
        if (
            isControlsLocked ||
            isMoreMenuOpen ||
            !playbackState.isPlaying ||
            !areControlsVisible
        ) {
            return@LaunchedEffect
        }

        delay(
            VideoControlsAutoHideMillis,
        )

        if (
            playbackState.isPlaying &&
            !isMoreMenuOpen &&
            !isControlsLocked
        ) {
            areControlsVisible = false
        }
    }

    BackHandler(
        enabled =
            isFullscreen ||
                isControlsLocked,
    ) {
        if (!isControlsLocked) {
            isFullscreen = false
        }
    }

    DisposableEffect(
        activity,
        isFullscreen,
    ) {
        val window =
            activity?.window

        val insetsController =
            window?.let { activityWindow ->
                WindowCompat.getInsetsController(
                    activityWindow,
                    activityWindow.decorView,
                )
            }

        val previousLightStatusBars =
            insetsController
                ?.isAppearanceLightStatusBars

        val previousLightNavigationBars =
            insetsController
                ?.isAppearanceLightNavigationBars

        val previousStatusBarColor =
            window?.statusBarColor

        val previousNavigationBarColor =
            window?.navigationBarColor

        window?.statusBarColor =
            AndroidColor.BLACK

        window?.navigationBarColor =
            AndroidColor.BLACK

        insetsController
            ?.isAppearanceLightStatusBars =
            false

        insetsController
            ?.isAppearanceLightNavigationBars =
            false

        if (
            activity != null &&
            isFullscreen
        ) {
            activity.requestedOrientation =
                ActivityInfo
                    .SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            insetsController
                ?.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            insetsController?.hide(
                WindowInsetsCompat
                    .Type
                    .systemBars(),
            )
        } else {
            insetsController?.show(
                WindowInsetsCompat
                    .Type
                    .systemBars(),
            )

            activity?.requestedOrientation =
                ActivityInfo
                    .SCREEN_ORIENTATION_UNSPECIFIED
        }

        onDispose {
            insetsController?.show(
                WindowInsetsCompat
                    .Type
                    .systemBars(),
            )

            previousStatusBarColor
                ?.let { color ->
                    window.statusBarColor =
                        color
                }

            previousNavigationBarColor
                ?.let { color ->
                    window.navigationBarColor =
                        color
                }

            previousLightStatusBars
                ?.let { wasLight ->
                    insetsController
                        ?.isAppearanceLightStatusBars =
                        wasLight
                }

            previousLightNavigationBars
                ?.let { wasLight ->
                    insetsController
                        ?.isAppearanceLightNavigationBars =
                        wasLight
                }

            activity?.requestedOrientation =
                ActivityInfo
                    .SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(
        activity,
        isFullscreen,
        areControlsVisible,
        isControlsLocked,
    ) {
        val window =
            activity?.window
                ?: return@LaunchedEffect

        val insetsController =
            WindowCompat.getInsetsController(
                window,
                window.decorView,
            )

        when {
            isFullscreen -> {
                return@LaunchedEffect
            }

            isControlsLocked -> {
                insetsController
                    .systemBarsBehavior =
                    WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                insetsController.hide(
                    WindowInsetsCompat
                        .Type
                        .systemBars(),
                )
            }

            else -> {
                insetsController.show(
                    WindowInsetsCompat
                        .Type
                        .navigationBars(),
                )

                if (areControlsVisible) {
                    insetsController.show(
                        WindowInsetsCompat
                            .Type
                            .statusBars(),
                    )
                } else {
                    insetsController.hide(
                        WindowInsetsCompat
                            .Type
                            .statusBars(),
                    )
                }
            }
        }
    }

    val playerView =
        remember(context) {
            PlayerView(context).apply {
                useController = false
                setBackgroundColor(
                    AndroidColor.BLACK,
                )
                keepScreenOn = true
            }
        }

    DisposableEffect(
        playerView,
        player,
    ) {
        playerView.player = player

        onDispose {
            playerView.player = null
        }
    }

    val durationMs =
        (
                playbackState.durationMs
                    ?: currentVideo?.durationMs
                    ?: 0L
                )
            .coerceAtLeast(0L)

    val runtimePositionMs =
        playbackState
            .positionMs
            .coerceAtLeast(0L)

    val safeRuntimePositionMs =
        if (durationMs > 0L) {
            runtimePositionMs.coerceAtMost(
                durationMs,
            )
        } else {
            runtimePositionMs
        }

    val displayedPositionMs =
        pendingSeekPositionMs
            ?: safeRuntimePositionMs

    val latestPlaybackPositionMs by
        rememberUpdatedState(
            playbackState.positionMs,
        )

    val latestOnSeek by
        rememberUpdatedState(
            onSeek,
        )

    val latestOnTogglePlayPause by
        rememberUpdatedState(
            onTogglePlayPause,
        )

    val canPrevious =
        queueState.hasPrevious ||
                (
                        queueState.repeatMode ==
                                RepeatMode.ALL &&
                                queueState
                                    .items
                                    .isNotEmpty()
                        )

    val canNext =
        queueState.hasNext ||
                (
                        queueState.repeatMode ==
                                RepeatMode.ALL &&
                                queueState
                                    .items
                                    .isNotEmpty()
                        )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Color.Black,
                ),
    ) {
        AndroidView(
            factory = {
                playerView
            },
            modifier =
                Modifier.fillMaxSize(),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(
                        currentVideo?.id,
                        durationMs,
                        isControlsLocked,
                    ) {
                        detectTapGestures(
                            onTap = {
                                if (!isControlsLocked) {
                                    toggleControls()
                                }
                            },
                            onDoubleTap = {
                                    offset ->
                                if (isControlsLocked) {
                                    return@detectTapGestures
                                }

                                val width =
                                    size.width
                                        .toFloat()

                                if (width <= 0f) {
                                    return@detectTapGestures
                                }

                                val currentPositionMs =
                                    latestPlaybackPositionMs
                                        .coerceAtLeast(
                                            0L,
                                        )

                                when {
                                    offset.x <
                                            width / 3f -> {
                                        latestOnSeek(
                                            (
                                                currentPositionMs -
                                                    10_000L
                                            )
                                                .coerceAtLeast(
                                                    0L,
                                                ),
                                        )
                                    }

                                    offset.x >
                                            width *
                                                2f /
                                                3f -> {
                                        val target =
                                            currentPositionMs +
                                                10_000L

                                        latestOnSeek(
                                            if (
                                                durationMs >
                                                0L
                                            ) {
                                                target
                                                    .coerceAtMost(
                                                        durationMs,
                                                    )
                                            } else {
                                                target
                                            },
                                        )
                                    }

                                    else -> {
                                        latestOnTogglePlayPause()
                                    }
                                }
                            },
                        )
                    },
        )

        if (isControlsLocked) {
            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.CenterStart,
                        )
                        .padding(
                            start = 12.dp,
                        )
                        .background(
                            color =
                                Color.Black.copy(
                                    alpha = 0.62f,
                                ),
                            shape =
                                RoundedCornerShape(
                                    16.dp,
                                ),
                        )
                        .size(
                            48.dp,
                        )
                        .pointerInput(
                            isControlsLocked,
                        ) {
                            detectTapGestures(
                                onPress = {
                                    val releasedBeforeTimeout =
                                        withTimeoutOrNull(
                                            ChildLockUnlockHoldMillis,
                                        ) {
                                            tryAwaitRelease()
                                        }

                                    if (
                                        releasedBeforeTimeout ==
                                        null &&
                                        isControlsLocked
                                    ) {
                                        hapticFeedback
                                            .performHapticFeedback(
                                                HapticFeedbackType
                                                    .LongPress,
                                            )

                                        openChildLockPatternVerification()
                                    }
                                },
                            )
                        },
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    imageVector =
                        Icons.Filled.Lock,
                    contentDescription =
                        "Hold to unlock child lock",
                    tint = Color.White,
                )
            }
        }

        if (areControlsVisible) {
            Row(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopCenter,
                        )
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(
                                alpha = 0.52f,
                            ),
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 6.dp,
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (isFullscreen) {
                            isFullscreen = false
                        } else {
                            onBack()
                        }
                    },
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.ArrowBack,
                        contentDescription =
                            if (isFullscreen) {
                                "Exit fullscreen"
                            } else {
                                "Back"
                            },
                        tint = Color.White,
                    )
                }

                Text(
                    text =
                        currentVideo
                            ?.title
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: currentVideo
                                ?.displayName
                            ?: "Video",
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                horizontal = 8.dp,
                            )
                            .basicMarquee(
                                iterations =
                                    Int.MAX_VALUE,
                            ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Clip,
                )

                IconButton(
                    onClick =
                        onSwitchToAudioOnly,
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.MusicNote,
                        contentDescription =
                            "Switch to audio only",
                        tint = Color.White,
                    )
                }

                IconButton(
                    onClick = {
                        isFullscreen =
                            !isFullscreen
                    },
                ) {
                    Icon(
                        imageVector =
                            if (isFullscreen) {
                                Icons.Filled
                                    .FullscreenExit
                            } else {
                                Icons.Filled
                                    .Fullscreen
                            },
                        contentDescription =
                            if (isFullscreen) {
                                "Exit fullscreen"
                            } else {
                                "Fullscreen"
                            },
                        tint = Color.White,
                    )
                }

                Box {
                    IconButton(
                        onClick = {
                            isMoreMenuOpen =
                                !isMoreMenuOpen
                        },
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.MoreVert,
                            contentDescription =
                                "More",
                            tint = Color.White,
                        )
                    }

                    DropdownMenu(
                        expanded =
                            isMoreMenuOpen,
                        onDismissRequest = {
                            isMoreMenuOpen = false
                        },
                    ) {
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
                            enabled =
                                currentVideo != null,
                            onClick = {
                                isMoreMenuOpen = false

                                currentVideo
                                    ?.let { video ->
                                        shareMedia(
                                            context = context,
                                            item = video,
                                        )
                                    }
                            },
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
                            enabled =
                                currentVideo != null,
                            onClick = {
                                isMoreMenuOpen = false
                                isDetailsDialogOpen =
                                    true
                            },
                        )

                        if (isFullscreen) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text =
                                            "Child lock",
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.Lock,
                                        contentDescription =
                                            null,
                                    )
                                },
                                onClick = {
                                    isMoreMenuOpen = false

                                    if (
                                        childLockPatternStore
                                            .hasPattern()
                                    ) {
                                        activateChildLock()
                                    } else {
                                        isSetChildLockPatternDialogOpen =
                                            true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (
            playbackState.status ==
            PlaybackStatus.PREPARING ||
            playbackState.status ==
            PlaybackStatus.BUFFERING
        ) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(
                            Alignment.Center,
                        )
                        .size(42.dp),
                color = Color.White,
            )
        }

        // Transport controls are rendered below the progress bar.

        if (
            playbackState.status ==
            PlaybackStatus.ERROR
        ) {
            Column(
                modifier =
                    Modifier
                        .align(
                            Alignment.Center,
                        )
                        .background(
                            Color.Black.copy(
                                alpha = 0.72f,
                            ),
                        )
                        .padding(20.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp,
                    ),
            ) {
                Text(
                    text =
                        playbackState
                            .error
                            ?.message
                            ?: "Unable to play this video.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    color = Color.White,
                )

                playbackState
                    .error
                    ?.diagnosticCode
                    ?.let { code ->
                        Text(
                            text = code,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                Color.White.copy(
                                    alpha = 0.7f,
                                ),
                        )
                    }
            }
        }

        if (areControlsVisible) {
            Column(
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter,
                        )
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(
                                alpha = 0.58f,
                            ),
                        )
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 10.dp,
                        ),
            ) {
                Slider(
                    value =
                        displayedPositionMs
                            .toFloat(),
                    onValueChange = { value ->
                        if (durationMs > 0L) {
                            pendingSeekPositionMs =
                                value
                                    .toLong()
                                    .coerceIn(
                                        0L,
                                        durationMs,
                                    )
                        }
                    },
                    onValueChangeFinished = {
                        pendingSeekPositionMs
                            ?.let { position ->
                                onSeek(position)
                            }

                        pendingSeekPositionMs =
                            null
                    },
                    valueRange =
                        0f..
                                durationMs
                                    .coerceAtLeast(
                                        1L,
                                    )
                                    .toFloat(),
                    enabled =
                        durationMs > 0L &&
                                playbackState
                                    .status !=
                                PlaybackStatus.ERROR,
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            formatVideoPlayerTime(
                                displayedPositionMs,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,
                        color =
                            Color.White.copy(
                                alpha = 0.82f,
                            ),
                    )

                    Spacer(
                        modifier =
                            Modifier.weight(1f),
                    )

                    Text(
                        text =
                            formatVideoPlayerTime(
                                durationMs,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,
                        color =
                            Color.White.copy(
                                alpha = 0.82f,
                            ),
                    )
                }

                if (
                    playbackState.status !=
                    PlaybackStatus.ERROR
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 2.dp,
                                ),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically,
                    ) {
                        VideoPlayerControlButton(
                            enabled = canPrevious,
                            icon =
                                Icons.Filled
                                    .SkipPrevious,
                            contentDescription =
                                "Previous video",
                            onClick = onPrevious,
                        )

                        VideoPlayerControlButton(
                            enabled =
                                currentVideo != null,
                            icon =
                                Icons.Filled
                                    .FastRewind,
                            contentDescription =
                                "Seek backward 10 seconds",
                            onClick = {
                                onSeek(
                                    (
                                            safeRuntimePositionMs -
                                                    10_000L
                                            )
                                        .coerceAtLeast(
                                            0L,
                                        ),
                                )
                            },
                        )

                        VideoPlayerControlButton(
                            enabled =
                                currentVideo != null,
                            icon =
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
                            onClick =
                                onTogglePlayPause,
                            prominent = true,
                        )

                        VideoPlayerControlButton(
                            enabled =
                                currentVideo != null,
                            icon =
                                Icons.Filled
                                    .FastForward,
                            contentDescription =
                                "Seek forward 10 seconds",
                            onClick = {
                                val requestedPosition =
                                    safeRuntimePositionMs +
                                            10_000L

                                onSeek(
                                    if (durationMs > 0L) {
                                        requestedPosition
                                            .coerceAtMost(
                                                durationMs,
                                            )
                                    } else {
                                        requestedPosition
                                    },
                                )
                            },
                        )

                        VideoPlayerControlButton(
                            enabled = canNext,
                            icon =
                                Icons.Filled
                                    .SkipNext,
                            contentDescription =
                                "Next video",
                            onClick = onNext,
                        )
                    }
                }
            }
        }
    }

    if (isSetChildLockPatternDialogOpen) {
        SetChildLockPatternDialog(
            onDismiss = {
                isSetChildLockPatternDialogOpen =
                    false
            },
            onPatternSaved = {
                    pattern ->
                childLockPatternStore.save(
                    pattern,
                )

                isSetChildLockPatternDialogOpen =
                    false

                activateChildLock()
            },
        )
    }


    if (isVerifyChildLockPatternDialogOpen) {
        VerifyChildLockPatternDialog(
            patternStore =
                childLockPatternStore,
            onDismiss = {
                isVerifyChildLockPatternDialogOpen =
                    false
            },
            onVerified = {
                isVerifyChildLockPatternDialogOpen =
                    false

                exitChildLock()
            },
        )
    }

    if (
        isDetailsDialogOpen &&
        currentVideo != null
    ) {
        MediaDetailsDialog(
            item = currentVideo,
            onDismiss = {
                isDetailsDialogOpen =
                    false
            },
        )
    }
}

@Composable
private fun VideoPlayerControlButton(
    enabled: Boolean,
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    prominent: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.size(
                if (prominent) {
                    60.dp
                } else {
                    50.dp
                },
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription =
                contentDescription,
            modifier =
                Modifier.size(
                    if (prominent) {
                        40.dp
                    } else {
                        30.dp
                    },
                ),
            tint =
                if (enabled) {
                    Color.White
                } else {
                    Color.White.copy(
                        alpha = 0.34f,
                    )
                },
        )
    }
}

private const val VideoControlsAutoHideMillis =
    3_000L

private const val ChildLockUnlockHoldMillis =
    2_000L

private fun Activity.stopLockTaskIfActive() {
    val activityManager =
        getSystemService(
            ActivityManager::class.java,
        )

    if (
        activityManager
            .lockTaskModeState !=
        ActivityManager.LOCK_TASK_MODE_NONE
    ) {
        stopLockTask()
    }
}

private fun formatVideoPlayerTime(
    positionMs: Long,
): String {
    val totalSeconds =
        positionMs
            .coerceAtLeast(0L) /
                1_000L

    val hours =
        totalSeconds / 3_600L

    val minutes =
        (totalSeconds % 3_600L) /
                60L

    val seconds =
        totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(
            hours,
            minutes,
            seconds,
        )
    } else {
        "%02d:%02d".format(
            minutes,
            seconds,
        )
    }
}

private tailrec fun Context.findVideoPlayerActivity():
        Activity? {
    return when (this) {
        is Activity ->
            this

        is ContextWrapper ->
            baseContext
                .findVideoPlayerActivity()

        else ->
            null
    }
}