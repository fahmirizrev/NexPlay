package com.nexplay.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.nexplay.app.data.media.MediaLibraryRepository
import com.nexplay.app.data.playlist.PlaylistRepository
import com.nexplay.app.data.preferences.NexPlayPreferencesRepository
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.domain.queue.QueueState
import com.nexplay.app.playback.PlaybackState
import com.nexplay.app.ui.theme.NexPlayThemeMode


internal enum class NexPlayRootTab {
    FOLDERS,
    PLAYHUB,
}

internal enum class NexPlayNestedLayer {
    FOLDER_DETAIL,
    PLAYLIST_DETAIL,
    QUEUE,
}

internal enum class NexPlayOverlayLayer {
    SEARCH,
}

internal enum class NexPlayVerticalLayer {
    AUDIO_PLAYER,
    VIDEO_PLAYER,
}


private const val NexPlayMotionDurationMillis = 200
private const val NexPlayRootTabMotionDurationMillis = 230
private const val NexPlayFolderDetailMotionDurationMillis = 230
private const val NexPlayPlaylistDetailMotionDurationMillis = 320
private const val NexPlaySearchMotionDurationMillis = 220
private const val NexPlayBottomBarMotionDurationMillis = 190

internal val NexPlayBottomBarHeight = 80.dp
private val NexPlayBottomBarSurfaceHeight = 64.dp

@Composable
internal fun NexPlayShell(
    rootTab: NexPlayRootTab,
    nestedLayer: NexPlayNestedLayer?,
    overlayLayer: NexPlayOverlayLayer?,
    verticalLayer: NexPlayVerticalLayer?,
    audioPlayerEnterFromRight: Boolean,
    selectedFolder: FolderInfo?,
    selectedPlaylistId: String?,
    mediaLibraryRepository: MediaLibraryRepository,
    playlistRepository:
        PlaylistRepository,
    preferencesRepository:
        NexPlayPreferencesRepository,
    queueState: QueueState,
    playbackState: PlaybackState,
    videoPlayer: Player,
    themeMode: NexPlayThemeMode,
    onThemeModeSelected:
        (NexPlayThemeMode) -> Unit,
    onSelectRootTab: (NexPlayRootTab) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    onCloseFolder: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onPlaySearchResult: (MediaItem) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenQueueFromPlayer: () -> Unit,
    onCloseQueue: () -> Unit,
    onOpenCurrentPlayer: () -> Unit,
    onOpenPlaylist:
        (String) -> Unit,
    onClosePlaylist: () -> Unit,
    onPlayPlaylist: (
        String,
        String,
        List<MediaItem>,
        Int,
    ) -> Unit,
    onQueueItemSelected: (
        Int,
        MediaItem,
    ) -> Unit,
    onOpenAudioPlayer: () -> Unit,
    onPlayAudio: (
        AudioItem,
        List<MediaItem>,
    ) -> Unit,
    onPlayVideo: (
        VideoItem,
        List<MediaItem>,
    ) -> Unit,
    onSwitchVideoToAudioOnly: () -> Unit,
    onSwitchAudioOnlyToVideo: () -> Unit,
    onTogglePlayerPlayPause: () -> Unit,
    onPlayerSeek: (Long) -> Unit,
    onPlayerPrevious: () -> Unit,
    onPlayerNext: () -> Unit,
    onTogglePlayerShuffle: () -> Unit,
    onCyclePlayerRepeat: () -> Unit,
    onTogglePlayerAbRepeat: () -> Unit,
    onStopAudio: () -> Unit,
    onCloseVerticalLayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFolderSelectionMode by
        remember {
            mutableStateOf(false)
        }

    val rootProgress by animateFloatAsState(
        targetValue =
            if (rootTab == NexPlayRootTab.FOLDERS) {
                0f
            } else {
                1f
            },
        animationSpec =
            tween(
                NexPlayRootTabMotionDurationMillis,
            ),
        label = "root-tab-progress",
    )

    val detailProgress by animateFloatAsState(
        targetValue =
            if (
                nestedLayer ==
                    NexPlayNestedLayer
                        .FOLDER_DETAIL ||
                nestedLayer ==
                    NexPlayNestedLayer
                        .PLAYLIST_DETAIL ||
                nestedLayer ==
                    NexPlayNestedLayer
                        .QUEUE
            ) {
                1f
            } else {
                0f
            },
        animationSpec =
            tween(
                NexPlayFolderDetailMotionDurationMillis,
            ),
        label = "folder-detail-progress",
    )

    val isBottomBarVisible =
        nestedLayer == null &&
                overlayLayer == null &&
                verticalLayer == null

    val isAudioMiniPlayerVisible =
        playbackState.currentMedia != null &&
                (
                    queueState.currentItem is AudioItem ||
                        queueState.currentItem is VideoItem
                ) &&
                overlayLayer == null &&
                verticalLayer !=
                NexPlayVerticalLayer.AUDIO_PLAYER &&
                verticalLayer !=
                NexPlayVerticalLayer.VIDEO_PLAYER &&
                (
                    nestedLayer ==
                        NexPlayNestedLayer.FOLDER_DETAIL ||
                        nestedLayer ==
                        NexPlayNestedLayer.PLAYLIST_DETAIL ||
                        nestedLayer ==
                        NexPlayNestedLayer.QUEUE ||
                        (
                            nestedLayer == null &&
                                rootTab ==
                                NexPlayRootTab.FOLDERS
                        )
                )

    val audioMiniPlayerBottomPadding =
        (
            if (isBottomBarVisible) {
                NexPlayBottomBarHeight +
                    8.dp
            } else {
                12.dp
            }
        ) +
            if (isFolderSelectionMode) {
                FolderDetailSelectionActionBarHeight
            } else {
                0.dp
            }

    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX =
                        -size.width *
                                0.07f *
                                detailProgress
                    alpha =
                        1f -
                                (0.12f * detailProgress)
                },
        ) {
            NexPlayRootLayer(
                rootProgress = rootProgress,
                mediaLibraryRepository =
                    mediaLibraryRepository,
                playlistRepository =
                    playlistRepository,
                preferencesRepository =
                    preferencesRepository,
                queueState = queueState,
                playbackState = playbackState,
                themeMode = themeMode,
                onThemeModeSelected =
                    onThemeModeSelected,
                onOpenFolder = onOpenFolder,
                onOpenSearch = onOpenSearch,
                onOpenQueue = onOpenQueue,
                onOpenCurrentPlayer =
                    onOpenCurrentPlayer,
                onOpenPlaylist =
                    onOpenPlaylist,
                onTogglePlayPause =
                    onTogglePlayerPlayPause,
                onPrevious = onPlayerPrevious,
                onNext = onPlayerNext,
            )
        }

        NexPlayFolderDetailLayer(
            visible =
                nestedLayer ==
                    NexPlayNestedLayer
                        .FOLDER_DETAIL,
            repository =
                mediaLibraryRepository,
            playlistRepository =
                playlistRepository,
            preferencesRepository =
                preferencesRepository,
            folder = selectedFolder,
            currentMediaUri =
                queueState.currentItem?.uri,
            onBack = onCloseFolder,
            onSelectionModeChanged = {
                    isSelectionMode ->
                isFolderSelectionMode =
                    isSelectionMode
            },
            onPlayAudio = onPlayAudio,
            onPlayVideo = onPlayVideo,
            modifier =
                Modifier.fillMaxSize(),
        )

        NexPlayPlaylistLayer(
            visible =
                nestedLayer ==
                    NexPlayNestedLayer
                        .PLAYLIST_DETAIL,
            playlistId =
                selectedPlaylistId,
            playlistRepository =
                playlistRepository,
            mediaLibraryRepository =
                mediaLibraryRepository,
            onBack = onClosePlaylist,
            onPlayPlaylist =
                onPlayPlaylist,
            modifier =
                Modifier.fillMaxSize(),
        )

        NexPlayQueueLayer(
            visible =
                nestedLayer ==
                        NexPlayNestedLayer.QUEUE,
            queueState = queueState,
            onBack = onCloseQueue,
            onPlayItem =
                onQueueItemSelected,
            modifier = Modifier.fillMaxSize(),
        )

        NexPlaySearchLayer(
            visible =
                overlayLayer ==
                        NexPlayOverlayLayer.SEARCH,
            repository = mediaLibraryRepository,
            onBack = onCloseSearch,
            onPlayResult = onPlaySearchResult,
            modifier = Modifier.fillMaxSize(),
        )

        NexPlayAudioPlayerLayer(
            visible =
                verticalLayer ==
                        NexPlayVerticalLayer
                            .AUDIO_PLAYER,
            enterFromRight =
                audioPlayerEnterFromRight,
            queueState = queueState,
            playbackState = playbackState,
            playlistRepository =
                playlistRepository,
            onBack = onCloseVerticalLayer,
            onTogglePlayPause =
                onTogglePlayerPlayPause,
            onSeek = onPlayerSeek,
            onPrevious = onPlayerPrevious,
            onNext = onPlayerNext,
            onToggleShuffle =
                onTogglePlayerShuffle,
            onCycleRepeat =
                onCyclePlayerRepeat,
            onToggleAbRepeat =
                onTogglePlayerAbRepeat,
            onOpenQueue =
                onOpenQueueFromPlayer,
            onSwitchToVideo =
                onSwitchAudioOnlyToVideo,
            modifier = Modifier.fillMaxSize(),
        )

        NexPlayVideoPlayerLayer(
            visible =
                verticalLayer ==
                        NexPlayVerticalLayer
                            .VIDEO_PLAYER,
            queueState = queueState,
            playbackState = playbackState,
            player = videoPlayer,
            onBack = onCloseVerticalLayer,
            onSwitchToAudioOnly =
                onSwitchVideoToAudioOnly,
            onTogglePlayPause =
                onTogglePlayerPlayPause,
            onSeek = onPlayerSeek,
            onPrevious = onPlayerPrevious,
            onNext = onPlayerNext,
            modifier = Modifier.fillMaxSize(),
        )

        NexPlayBottomBarLayer(

            visible = isBottomBarVisible,
            currentTab = rootTab,
            onFoldersSelected = {
                onSelectRootTab(
                    NexPlayRootTab.FOLDERS,
                )
            },
            onPlayhubSelected = {
                onSelectRootTab(
                    NexPlayRootTab.PLAYHUB,
                )
            },
            modifier =
                Modifier.align(
                    Alignment.BottomCenter,
                ),
        )

        AnimatedVisibility(
            visible =
                isAudioMiniPlayerVisible,
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter,
                    )
                    .padding(
                        start =
                            NexPlaySpacing
                                .screenPaddingX,
                        end =
                            NexPlaySpacing
                                .screenPaddingX,
                        bottom =
                            audioMiniPlayerBottomPadding,
                    ),
            enter =
                slideInVertically(
                    animationSpec =
                        tween(
                            NexPlayBottomBarMotionDurationMillis,
                        ),
                    initialOffsetY = {
                        it / 2
                    },
                ) +
                        fadeIn(
                            animationSpec =
                                tween(
                                    NexPlayBottomBarMotionDurationMillis,
                                ),
                        ),
            exit =
                slideOutVertically(
                    animationSpec =
                        tween(
                            NexPlayBottomBarMotionDurationMillis,
                        ),
                    targetOffsetY = {
                        it / 2
                    },
                ) +
                        fadeOut(
                            animationSpec =
                                tween(
                                    NexPlayBottomBarMotionDurationMillis,
                                ),
                        ),
        ) {
            AudioMiniPlayer(
                queueState = queueState,
                playbackState =
                    playbackState,
                onExpand =
                    onOpenAudioPlayer,
                onTogglePlayPause =
                    onTogglePlayerPlayPause,
                onStop =
                    onStopAudio,
            )
        }
    }
}

@Composable
private fun NexPlayRootLayer(
    rootProgress: Float,
    mediaLibraryRepository: MediaLibraryRepository,
    playlistRepository:
        PlaylistRepository,
    preferencesRepository:
        NexPlayPreferencesRepository,
    queueState: QueueState,
    playbackState: PlaybackState,
    themeMode: NexPlayThemeMode,
    onThemeModeSelected:
        (NexPlayThemeMode) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX =
                        -size.width * rootProgress
                },
        ) {
            FoldersScreen(
                repository = mediaLibraryRepository,
                preferencesRepository =
                    preferencesRepository,
                onOpenSearch = onOpenSearch,
                onOpenFolder = onOpenFolder,
                themeMode = themeMode,
                onThemeModeSelected =
                    onThemeModeSelected,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX =
                        size.width *
                                (1f - rootProgress)
                },
        ) {
            PlayHubScreen(
                queueState = queueState,
                playbackState = playbackState,
                playlistRepository =
                    playlistRepository,
                onOpenSearch = onOpenSearch,
                onOpenQueue = onOpenQueue,
                onOpenCurrentPlayer =
                    onOpenCurrentPlayer,
                onOpenPlaylist =
                    onOpenPlaylist,
                onTogglePlayPause =
                    onTogglePlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
            )
        }
    }
}

@Composable
private fun NexPlayFolderDetailLayer(
    visible: Boolean,
    repository:
        MediaLibraryRepository,
    playlistRepository:
        PlaylistRepository,
    preferencesRepository:
        NexPlayPreferencesRepository,
    folder: FolderInfo?,
    currentMediaUri: String?,
    onBack: () -> Unit,
    onSelectionModeChanged:
        (Boolean) -> Unit,
    onPlayAudio: (
        AudioItem,
        List<MediaItem>,
    ) -> Unit,
    onPlayVideo: (
        VideoItem,
        List<MediaItem>,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            slideInHorizontally(
                animationSpec =
                    tween(
                        NexPlayFolderDetailMotionDurationMillis,
                    ),
                initialOffsetX = { it / 4 },
            ) +
                    fadeIn(
                        animationSpec =
                            tween(
                                NexPlayFolderDetailMotionDurationMillis,
                            ),
                    ),
        exit =
            slideOutHorizontally(
                animationSpec =
                    tween(
                        NexPlayFolderDetailMotionDurationMillis,
                    ),
                targetOffsetX = { it / 4 },
            ) +
                    fadeOut(
                        animationSpec =
                            tween(
                                NexPlayFolderDetailMotionDurationMillis,
                            ),
                    ),
    ) {
        NexPlayLayerSurface {
            FolderDetailScreen(
                repository = repository,
                playlistRepository =
                    playlistRepository,
                preferencesRepository =
                    preferencesRepository,
                folder = folder,
                currentMediaUri =
                    currentMediaUri,
                onBack = onBack,
                onSelectionModeChanged =
                    onSelectionModeChanged,
                onPlayAudio = onPlayAudio,
                onPlayVideo = onPlayVideo,
            )
        }
    }
}

@Composable
private fun NexPlayPlaylistLayer(
    visible: Boolean,
    playlistId: String?,
    playlistRepository:
        PlaylistRepository,
    mediaLibraryRepository:
        MediaLibraryRepository,
    onBack: () -> Unit,
    onPlayPlaylist: (
        String,
        String,
        List<MediaItem>,
        Int,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible =
            visible &&
                playlistId != null,
        modifier = modifier,
        enter =
            slideInHorizontally(
                animationSpec =
                    tween(
                        NexPlayPlaylistDetailMotionDurationMillis,
                    ),
                initialOffsetX = {
                    it / 4
                },
            ) +
                fadeIn(
                    animationSpec =
                        tween(
                            NexPlayPlaylistDetailMotionDurationMillis,
                        ),
                ),
        exit =
            slideOutHorizontally(
                animationSpec =
                    tween(
                        NexPlayPlaylistDetailMotionDurationMillis,
                    ),
                targetOffsetX = {
                    it / 4
                },
            ) +
                fadeOut(
                    animationSpec =
                        tween(
                            NexPlayPlaylistDetailMotionDurationMillis,
                        ),
                ),
    ) {
        playlistId?.let { resolvedPlaylistId ->
            NexPlayLayerSurface {
                PlaylistDetailScreen(
                    playlistId =
                        resolvedPlaylistId,
                    playlistRepository =
                        playlistRepository,
                    mediaLibraryRepository =
                        mediaLibraryRepository,
                    onBack = onBack,
                    onPlayPlaylist =
                        onPlayPlaylist,
                )
            }
        }
    }
}

@Composable
private fun NexPlayQueueLayer(
    visible: Boolean,
    queueState: QueueState,
    onBack: () -> Unit,
    onPlayItem: (
        Int,
        MediaItem,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            slideInHorizontally(
                animationSpec =
                    tween(
                        NexPlayFolderDetailMotionDurationMillis,
                    ),
                initialOffsetX = { it / 4 },
            ) +
                    fadeIn(
                        animationSpec =
                            tween(
                                NexPlayFolderDetailMotionDurationMillis,
                            ),
                    ),
        exit =
            slideOutHorizontally(
                animationSpec =
                    tween(
                        NexPlayFolderDetailMotionDurationMillis,
                    ),
                targetOffsetX = { it / 4 },
            ) +
                    fadeOut(
                        animationSpec =
                            tween(
                                NexPlayFolderDetailMotionDurationMillis,
                            ),
                    ),
    ) {
        NexPlayLayerSurface {
            PlayHubQueueScreen(
                queueState = queueState,
                onBack = onBack,
                onPlayItem = onPlayItem,
            )
        }
    }
}

@Composable
private fun NexPlaySearchLayer(
    visible: Boolean,
    repository: MediaLibraryRepository,
    onBack: () -> Unit,
    onPlayResult: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            slideInVertically(
                animationSpec =
                    tween(
                        NexPlaySearchMotionDurationMillis,
                    ),
                initialOffsetY = { -it / 4 },
            ) +
                    fadeIn(
                        animationSpec =
                            tween(
                                NexPlaySearchMotionDurationMillis,
                            ),
                    ),
        exit =
            slideOutVertically(
                animationSpec =
                    tween(
                        NexPlaySearchMotionDurationMillis,
                    ),
                targetOffsetY = { -it / 4 },
            ) +
                    fadeOut(
                        animationSpec =
                            tween(
                                NexPlaySearchMotionDurationMillis,
                            ),
                    ),
    ) {
        NexPlayLayerSurface {
            SearchScreen(
                repository = repository,
                onBack = onBack,
                onPlayResult = onPlayResult,
            )
        }
    }
}

@Composable
private fun NexPlayAudioPlayerLayer(
    visible: Boolean,
    enterFromRight: Boolean,
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
    onSwitchToVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            if (enterFromRight) {
                slideInHorizontally(
                    animationSpec =
                        tween(
                            NexPlayFolderDetailMotionDurationMillis,
                        ),
                    initialOffsetX = {
                        it / 4
                    },
                ) +
                    fadeIn(
                        animationSpec =
                            tween(
                                NexPlayFolderDetailMotionDurationMillis,
                            ),
                    )
            } else {
                slideInVertically(
                    animationSpec =
                        tween(
                            NexPlayMotionDurationMillis,
                        ),
                    initialOffsetY = { it },
                )
            },
        exit =
            slideOutVertically(
                animationSpec =
                    tween(
                        NexPlayMotionDurationMillis,
                    ),
                targetOffsetY = { it },
            ),
    ) {
        NexPlayLayerSurface {
            AudioPlayerScreen(
                queueState = queueState,
                playbackState =
                    playbackState,
                playlistRepository =
                    playlistRepository,
                onBack = onBack,
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
                onOpenQueue =
                    onOpenQueue,
                onSwitchToVideo =
                    if (
                        queueState.currentItem
                        is VideoItem
                    ) {
                        onSwitchToVideo
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun NexPlayVideoPlayerLayer(
    visible: Boolean,
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
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            slideInVertically(
                animationSpec =
                    tween(
                        NexPlayMotionDurationMillis,
                    ),
                initialOffsetY = { it },
            ),
        exit =
            slideOutVertically(
                animationSpec =
                    tween(
                        NexPlayMotionDurationMillis,
                    ),
                targetOffsetY = { it },
            ),
    ) {
        VideoPlayerScreen(
            queueState = queueState,
            playbackState = playbackState,
            player = player,
            onBack = onBack,
            onSwitchToAudioOnly =
                onSwitchToAudioOnly,
            onTogglePlayPause =
                onTogglePlayPause,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
        )
    }
}

@Composable
private fun NexPlayLayerSurface(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
private fun NexPlayBottomBarLayer(
    visible: Boolean,
    currentTab: NexPlayRootTab,
    onFoldersSelected: () -> Unit,
    onPlayhubSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            slideInVertically(
                animationSpec =
                    tween(
                        NexPlayBottomBarMotionDurationMillis,
                    ),
                initialOffsetY = { it },
            ),
        exit =
            slideOutVertically(
                animationSpec =
                    tween(
                        NexPlayBottomBarMotionDurationMillis,
                    ),
                targetOffsetY = { it },
            ),
    ) {
        NexPlayBottomBar(
            currentTab = currentTab,
            onFoldersSelected = onFoldersSelected,
            onPlayhubSelected = onPlayhubSelected,
        )
    }
}

@Composable
private fun NexPlayBottomBar(
    currentTab: NexPlayRootTab,
    onFoldersSelected: () -> Unit,
    onPlayhubSelected: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(NexPlayBottomBarHeight)
            .padding(
                horizontal =
                    NexPlaySpacing.screenPaddingX,
                vertical = 8.dp,
            ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(NexPlayBottomBarSurfaceHeight),
            shape = MaterialTheme.shapes.large,
            color =
                MaterialTheme.colorScheme.background,
            border =
                BorderStroke(
                    width = 2.dp,
                    color =
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.70f,
                        ),
                ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
            ) {
                NexPlayBottomBarItem(
                    label = "Folders",
                    icon = Icons.Outlined.Folder,
                    selectedIcon = Icons.Filled.Folder,
                    selected =
                        currentTab ==
                                NexPlayRootTab.FOLDERS,
                    modifier = Modifier.weight(1f),
                    onClick = onFoldersSelected,
                )

                NexPlayBottomBarItem(
                    label = "Playhub",
                    icon =
                        Icons.AutoMirrored.Outlined.QueueMusic,
                    selectedIcon =
                        Icons.AutoMirrored.Filled.QueueMusic,
                    selected =
                        currentTab ==
                                NexPlayRootTab.PLAYHUB,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayhubSelected,
                )
            }
        }
    }
}

@Composable
private fun NexPlayBottomBarItem(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector =
                if (selected) {
                    selectedIcon
                } else {
                    icon
                },
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = color,
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = label,
            style =
                MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}