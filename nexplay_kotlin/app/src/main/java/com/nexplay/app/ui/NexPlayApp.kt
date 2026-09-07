package com.nexplay.app.ui

import android.content.Intent
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexplay.app.ExternalMediaOpenRequest
import com.nexplay.app.NexPlayApplication
import com.nexplay.app.data.media.MediaLibraryRepository
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.domain.queue.FolderContext
import com.nexplay.app.domain.queue.PlaylistContext
import com.nexplay.app.domain.queue.RepeatMode
import com.nexplay.app.domain.queue.SingleItemContext
import com.nexplay.app.playback.NexPlayPlaybackService
import com.nexplay.app.ui.theme.NexPlayThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NexPlayBackExitWindowMillis = 2_000L
private const val NexPlayBackExitDebounceMillis = 700L

@Composable
internal fun NexPlayApp(
    themeMode: NexPlayThemeMode,
    onThemeModeSelected:
        (NexPlayThemeMode) -> Unit,
    externalMediaRequest:
        ExternalMediaOpenRequest?,
    onExternalMediaConsumed:
        (Long) -> Unit,
) {
    val context = LocalContext.current
    val activity =
        context as? ComponentActivity

    val mediaLibraryRepository =
        remember(context.applicationContext) {
            MediaLibraryRepository(
                context.applicationContext,
            )
        }

    val scope =
        rememberCoroutineScope()

    DisposableEffect(
        activity,
        mediaLibraryRepository,
    ) {
        val lifecycle =
            activity?.lifecycle

        var shouldRefreshMediaOnResume =
            false

        val observer =
            LifecycleEventObserver {
                    _,
                    event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        shouldRefreshMediaOnResume =
                            true
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        if (
                            shouldRefreshMediaOnResume
                        ) {
                            shouldRefreshMediaOnResume =
                                false

                            scope.launch(
                                Dispatchers.IO,
                            ) {
                                mediaLibraryRepository
                                    .refresh()
                            }
                        }
                    }

                    else -> Unit
                }
            }

        lifecycle?.addObserver(
            observer,
        )

        onDispose {
            lifecycle?.removeObserver(
                observer,
            )
        }
    }

    val application =
        remember(
            context.applicationContext,
        ) {
            context.applicationContext
                as NexPlayApplication
        }

    val playbackCoordinator =
        remember(
            application,
        ) {
            application
                .playbackCoordinator
        }

    val playlistRepository =
        remember(
            application,
        ) {
            application
                .playlistRepository
        }

    val preferencesRepository =
        remember(
            application,
        ) {
            application
                .preferencesRepository
        }

    val queueState by
        playbackCoordinator
            .queueState
            .collectAsState()

    val playbackState by
        playbackCoordinator
            .playbackState
            .collectAsState()

    val hasPlaybackRuntime =
        playbackState.currentMedia != null

    LaunchedEffect(
        hasPlaybackRuntime,
    ) {
        val serviceIntent =
            Intent(
                context.applicationContext,
                NexPlayPlaybackService::class.java,
            )

        if (hasPlaybackRuntime) {
            context.applicationContext
                .startService(
                    serviceIntent,
                )
        } else {
            context.applicationContext
                .stopService(
                    serviceIntent,
                )
        }
    }

    var rootTab by remember {
        mutableStateOf(NexPlayRootTab.FOLDERS)
    }
    var nestedLayer by remember {
        mutableStateOf<NexPlayNestedLayer?>(null)
    }
    var overlayLayer by remember {
        mutableStateOf<NexPlayOverlayLayer?>(null)
    }
    var verticalLayer by rememberSaveable {
        mutableStateOf<NexPlayVerticalLayer?>(null)
    }
    var selectedFolder by remember {
        mutableStateOf<FolderInfo?>(null)
    }

    var selectedPlaylistId by
        rememberSaveable {
            mutableStateOf<String?>(null)
        }

    var queueReturnNestedLayer by
        remember {
            mutableStateOf<NexPlayNestedLayer?>(
                null,
            )
        }

    var queueReturnOverlayLayer by
        remember {
            mutableStateOf<NexPlayOverlayLayer?>(
                null,
            )
        }

    var queueReturnVerticalLayer by
        remember {
            mutableStateOf<NexPlayVerticalLayer?>(
                null,
            )
        }

    var isAudioPlayerEnteringFromQueue by
        remember {
            mutableStateOf(false)
        }

    LaunchedEffect(
        verticalLayer,
        nestedLayer,
    ) {
        if (
            isAudioPlayerEnteringFromQueue &&
            verticalLayer !=
            NexPlayVerticalLayer.AUDIO_PLAYER &&
            nestedLayer !=
            NexPlayNestedLayer.QUEUE
        ) {
            isAudioPlayerEnteringFromQueue =
                false
        }
    }

    LaunchedEffect(
        queueState.currentIndex,
        queueState.playbackContext,
    ) {
        if (
            queueState.playbackContext
            is PlaylistContext &&
            verticalLayer ==
            NexPlayVerticalLayer
                .VIDEO_PLAYER
        ) {
            verticalLayer =
                NexPlayVerticalLayer
                    .AUDIO_PLAYER
        }
    }

    var exitPromptDeadline by remember {
        mutableLongStateOf(0L)
    }
    var backExitAllowedAfter by remember {
        mutableLongStateOf(0L)
    }
    var showExitPrompt by remember {
        mutableStateOf(false)
    }

    val isFoldersRoot =
        rootTab == NexPlayRootTab.FOLDERS &&
                nestedLayer == null &&
                overlayLayer == null &&
                verticalLayer == null

    fun resetExitPrompt() {
        exitPromptDeadline = 0L
        backExitAllowedAfter = 0L
        showExitPrompt = false
    }

    LaunchedEffect(
        externalMediaRequest
            ?.requestId,
    ) {
        val request =
            externalMediaRequest
                ?: return@LaunchedEffect

        playbackCoordinator.setQueue(
            items =
                listOf(
                    request.item,
                ),
            currentIndex = 0,
            playbackContext =
                SingleItemContext,
        )

        verticalLayer =
            when (request.item) {
                is AudioItem ->
                    NexPlayVerticalLayer
                        .AUDIO_PLAYER

                is VideoItem ->
                    NexPlayVerticalLayer
                        .VIDEO_PLAYER
            }

        onExternalMediaConsumed(
            request.requestId,
        )

        resetExitPrompt()
    }

    fun closeVerticalLayer() {
        if (
            verticalLayer ==
            NexPlayVerticalLayer.VIDEO_PLAYER
        ) {
            playbackCoordinator.stopPlayback()
        }

        verticalLayer = null
        resetExitPrompt()
    }

    LaunchedEffect(isFoldersRoot) {
        if (!isFoldersRoot) {
            resetExitPrompt()
        }
    }

    LaunchedEffect(exitPromptDeadline) {
        val deadline = exitPromptDeadline
        if (deadline == 0L) {
            return@LaunchedEffect
        }

        val remaining =
            (deadline - SystemClock.elapsedRealtime())
                .coerceAtLeast(0L)

        delay(remaining)

        if (
            exitPromptDeadline == deadline &&
            SystemClock.elapsedRealtime() >= deadline
        ) {
            resetExitPrompt()
        }
    }

    BackHandler {
        when {
            verticalLayer != null -> {
                closeVerticalLayer()
            }

            overlayLayer != null -> {
                overlayLayer = null
                resetExitPrompt()
            }

            nestedLayer != null -> {
                when (
                    nestedLayer
                ) {
                    NexPlayNestedLayer.QUEUE -> {
                        isAudioPlayerEnteringFromQueue =
                            queueReturnVerticalLayer ==
                            NexPlayVerticalLayer
                                .AUDIO_PLAYER

                        nestedLayer =
                            queueReturnNestedLayer
                        overlayLayer =
                            queueReturnOverlayLayer
                        verticalLayer =
                            queueReturnVerticalLayer
                        queueReturnNestedLayer =
                            null
                        queueReturnOverlayLayer =
                            null
                        queueReturnVerticalLayer =
                            null
                    }

                    NexPlayNestedLayer.PLAYLIST_DETAIL -> {
                        selectedPlaylistId =
                            null
                        nestedLayer = null
                    }

                    else -> {
                        nestedLayer = null
                    }
                }

                resetExitPrompt()
            }

            rootTab == NexPlayRootTab.PLAYHUB -> {
                rootTab = NexPlayRootTab.FOLDERS
                resetExitPrompt()
            }

            else -> {
                val now = SystemClock.elapsedRealtime()
                val isInsideExitWindow =
                    exitPromptDeadline != 0L &&
                            now <= exitPromptDeadline
                val isAfterDebounce =
                    backExitAllowedAfter != 0L &&
                            now >= backExitAllowedAfter

                if (
                    isInsideExitWindow &&
                    isAfterDebounce
                ) {
                    resetExitPrompt()
                    activity?.finish()
                } else {
                    exitPromptDeadline =
                        now + NexPlayBackExitWindowMillis
                    backExitAllowedAfter =
                        now + NexPlayBackExitDebounceMillis
                    showExitPrompt = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor =
            if (
                verticalLayer ==
                NexPlayVerticalLayer.VIDEO_PLAYER
            ) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.background
            },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NexPlayShell(
                rootTab = rootTab,
                nestedLayer = nestedLayer,
                overlayLayer = overlayLayer,
                verticalLayer = verticalLayer,
                audioPlayerEnterFromRight =
                    isAudioPlayerEnteringFromQueue,
                selectedFolder = selectedFolder,
                selectedPlaylistId =
                    selectedPlaylistId,
                mediaLibraryRepository =
                    mediaLibraryRepository,
                playlistRepository =
                    playlistRepository,
                preferencesRepository =
                    preferencesRepository,
                queueState = queueState,
                playbackState = playbackState,
                videoPlayer =
                    playbackCoordinator
                        .videoViewDelegate,
                themeMode = themeMode,
                onThemeModeSelected =
                    onThemeModeSelected,
                onSelectRootTab = { nextTab ->

                    if (
                        nestedLayer == null &&
                        overlayLayer == null &&
                        verticalLayer == null
                    ) {
                        rootTab = nextTab
                        resetExitPrompt()
                    }
                },
                onOpenFolder = { folder ->
                    selectedFolder = folder
                    nestedLayer =
                        NexPlayNestedLayer.FOLDER_DETAIL
                    resetExitPrompt()
                },
                onCloseFolder = {
                    nestedLayer = null
                },
                onOpenSearch = {
                    overlayLayer =
                        NexPlayOverlayLayer.SEARCH
                    resetExitPrompt()
                },
                onCloseSearch = {
                    overlayLayer = null
                },
                onPlaySearchResult = { item ->
                    val sourceFolder = item.folder

                    val sourceItems =
                        sourceFolder
                            ?.let(
                                mediaLibraryRepository::getMediaInFolder,
                            )
                            .orEmpty()

                    val folderQueue: List<MediaItem> =
                        sourceItems

                    val hasSourceItem =
                        folderQueue.any { candidate ->
                            candidate.id == item.id ||
                                    candidate.uri == item.uri
                        }

                    val queueItems =
                        if (hasSourceItem) {
                            folderQueue
                        } else {
                            listOf(item)
                        }

                    val currentIndex =
                        queueItems.indexOfFirst { candidate ->
                            candidate.id == item.id ||
                                    candidate.uri == item.uri
                        }

                    if (currentIndex >= 0) {
                        playbackCoordinator.setQueue(
                            items = queueItems,
                            currentIndex = currentIndex,
                            playbackContext =
                                if (
                                    hasSourceItem &&
                                    sourceFolder != null
                                ) {
                                    FolderContext(
                                        sourceFolder,
                                    )
                                } else {
                                    SingleItemContext
                                },
                        )

                        verticalLayer =
                            when (item) {
                                is AudioItem ->
                                    NexPlayVerticalLayer
                                        .AUDIO_PLAYER

                                is VideoItem ->
                                    NexPlayVerticalLayer
                                        .VIDEO_PLAYER
                            }

                        resetExitPrompt()
                    }
                },
                onOpenQueue = {
                    isAudioPlayerEnteringFromQueue =
                        false
                    queueReturnNestedLayer =
                        nestedLayer
                    queueReturnOverlayLayer =
                        overlayLayer
                    queueReturnVerticalLayer =
                        null
                    overlayLayer = null
                    nestedLayer =
                        NexPlayNestedLayer.QUEUE
                    resetExitPrompt()
                },
                onOpenQueueFromPlayer = {
                    isAudioPlayerEnteringFromQueue =
                        false
                    queueReturnNestedLayer =
                        nestedLayer
                            ?.takeIf {
                                it !=
                                    NexPlayNestedLayer.QUEUE
                            }
                    queueReturnOverlayLayer =
                        overlayLayer
                    queueReturnVerticalLayer =
                        verticalLayer
                    overlayLayer = null
                    verticalLayer = null
                    nestedLayer =
                        NexPlayNestedLayer.QUEUE
                    resetExitPrompt()
                },
                onCloseQueue = {
                    isAudioPlayerEnteringFromQueue =
                        queueReturnVerticalLayer ==
                        NexPlayVerticalLayer
                            .AUDIO_PLAYER

                    nestedLayer =
                        queueReturnNestedLayer
                    overlayLayer =
                        queueReturnOverlayLayer
                    verticalLayer =
                        queueReturnVerticalLayer
                    queueReturnNestedLayer =
                        null
                    queueReturnOverlayLayer =
                        null
                    queueReturnVerticalLayer =
                        null
                    resetExitPrompt()
                },
                onOpenCurrentPlayer = {
                    when {
                        queueState.currentItem ==
                            null -> Unit

                        queueState.playbackContext
                        is PlaylistContext -> {
                            verticalLayer =
                                NexPlayVerticalLayer
                                    .AUDIO_PLAYER
                            resetExitPrompt()
                        }

                        queueState.currentItem
                        is AudioItem -> {
                            verticalLayer =
                                NexPlayVerticalLayer
                                    .AUDIO_PLAYER
                            resetExitPrompt()
                        }

                        queueState.currentItem
                        is VideoItem -> {
                            verticalLayer =
                                NexPlayVerticalLayer
                                    .VIDEO_PLAYER
                            resetExitPrompt()
                        }
                    }
                },
                onOpenPlaylist = {
                        playlistId ->
                    selectedPlaylistId =
                        playlistId
                    nestedLayer =
                        NexPlayNestedLayer
                            .PLAYLIST_DETAIL
                    resetExitPrompt()
                },
                onClosePlaylist = {
                    selectedPlaylistId =
                        null
                    nestedLayer = null
                    resetExitPrompt()
                },
                onPlayPlaylist = {
                        playlistId,
                        playlistName,
                        items,
                        currentIndex ->
                    if (
                        items.isNotEmpty() &&
                        currentIndex in
                        items.indices
                    ) {
                        playbackCoordinator
                            .setQueue(
                                items = items,
                                currentIndex =
                                    currentIndex,
                                playbackContext =
                                    PlaylistContext(
                                        playlistId =
                                            playlistId,
                                        playlistName =
                                            playlistName,
                                    ),
                            )

                        verticalLayer =
                            NexPlayVerticalLayer
                                .AUDIO_PLAYER

                        resetExitPrompt()
                    }
                },
                onQueueItemSelected = {
                        index,
                        item ->
                    playbackCoordinator.playAt(
                        index,
                    )

                    verticalLayer =
                        if (
                            queueState.playbackContext
                            is PlaylistContext
                        ) {
                            NexPlayVerticalLayer
                                .AUDIO_PLAYER
                        } else {
                            when (item) {
                                is AudioItem ->
                                    NexPlayVerticalLayer
                                        .AUDIO_PLAYER

                                is VideoItem ->
                                    NexPlayVerticalLayer
                                        .VIDEO_PLAYER
                            }
                        }

                    resetExitPrompt()
                },
                onOpenAudioPlayer = {
                    if (
                        queueState.currentItem
                        is AudioItem ||
                        queueState.currentItem
                        is VideoItem
                    ) {
                        verticalLayer =
                            NexPlayVerticalLayer
                                .AUDIO_PLAYER
                        resetExitPrompt()
                    }
                },
                onPlayAudio = {
                        item: AudioItem,
                        items: List<MediaItem> ->
                    val folder =
                        selectedFolder
                            ?: item.folder

                    val currentIndex =
                        items.indexOfFirst {
                                candidate ->
                            candidate.id == item.id ||
                                    candidate.uri ==
                                    item.uri
                        }

                    if (
                        folder != null &&
                        currentIndex >= 0
                    ) {
                        playbackCoordinator.setQueue(
                            items = items,
                            currentIndex =
                                currentIndex,
                            playbackContext =
                                FolderContext(
                                    folder,
                                ),
                        )

                        verticalLayer =
                            NexPlayVerticalLayer
                                .AUDIO_PLAYER
                        resetExitPrompt()
                    }
                },
                onPlayVideo = {
                        item: VideoItem,
                        items: List<MediaItem> ->
                    val folder =
                        selectedFolder
                            ?: item.folder

                    val currentIndex =
                        items.indexOfFirst {
                                candidate ->
                            candidate.id == item.id ||
                                    candidate.uri ==
                                    item.uri
                        }

                    if (
                        folder != null &&
                        currentIndex >= 0
                    ) {
                        playbackCoordinator.setQueue(
                            items = items,
                            currentIndex =
                                currentIndex,
                            playbackContext =
                                FolderContext(
                                    folder,
                                ),
                        )

                        verticalLayer =
                            NexPlayVerticalLayer
                                .VIDEO_PLAYER
                        resetExitPrompt()
                    }
                },
                onSwitchVideoToAudioOnly = {
                    if (
                        queueState.currentItem
                        is VideoItem
                    ) {
                        verticalLayer =
                            NexPlayVerticalLayer
                                .AUDIO_PLAYER
                        resetExitPrompt()
                    }
                },
                onSwitchAudioOnlyToVideo = {
                    if (
                        queueState.currentItem
                        is VideoItem
                    ) {
                        verticalLayer =
                            NexPlayVerticalLayer
                                .VIDEO_PLAYER
                        resetExitPrompt()
                    }
                },
                onTogglePlayerPlayPause = {
                    if (playbackState.isPlaying) {
                        playbackCoordinator.pause()
                    } else {
                        playbackCoordinator.play()
                    }
                },
                onPlayerSeek = {
                        positionMs ->
                    playbackCoordinator.seek(
                        positionMs,
                    )
                },
                onPlayerPrevious = {
                    playbackCoordinator.previous()
                },
                onPlayerNext = {
                    playbackCoordinator.next()
                },
                onTogglePlayerShuffle = {
                    playbackCoordinator
                        .toggleShuffle()
                },
                onCyclePlayerRepeat = {
                    val nextRepeatMode =
                        when (
                            queueState.repeatMode
                        ) {
                            RepeatMode.OFF ->
                                RepeatMode.ONE

                            RepeatMode.ONE ->
                                RepeatMode.ALL

                            RepeatMode.ALL ->
                                RepeatMode.OFF
                        }

                    playbackCoordinator.setRepeat(
                        nextRepeatMode,
                    )
                },
                onTogglePlayerAbRepeat = {
                    playbackCoordinator
                        .toggleAbRepeatMarker()
                },
                onStopAudio = {
                    playbackCoordinator.stopPlayback()
                    resetExitPrompt()
                },
                onCloseVerticalLayer = {
                    closeVerticalLayer()
                },
                modifier = Modifier.fillMaxSize(),
            )


            if (showExitPrompt) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom =
                                NexPlayBottomBarHeight +
                                        12.dp,
                        ),
                    color =
                        MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text =
                            "Tap back again to exit NexPlay",
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 8.dp,
                        ),
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}