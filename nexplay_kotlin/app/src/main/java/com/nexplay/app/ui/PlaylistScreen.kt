package com.nexplay.app.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nexplay.app.data.media.MediaLibraryRepository
import com.nexplay.app.data.playlist.PlaylistRepository
import com.nexplay.app.data.playlist.PlaylistSummary
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
internal fun PlayHubPlaylistSummaryRow(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        8.dp,
                    ),
                )
                .clickable(
                    onClick = onClick,
                )
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
                    imageVector =
                        Icons.AutoMirrored
                            .Outlined
                            .QueueMusic,
                    contentDescription =
                        null,
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
                text = playlist.name,
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
                text =
                    "${playlist.itemCount} " +
                            if (
                                playlist.itemCount ==
                                1
                            ) {
                                "track"
                            } else {
                                "tracks"
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
        }

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
                contentDescription =
                    null,
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
internal fun PlaylistDetailScreen(
    playlistId: String,
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
    val detailFlow =
        remember(
            playlistRepository,
            playlistId,
        ) {
            playlistRepository
                .observePlaylist(
                    playlistId,
                )
        }

    val detail by
    detailFlow.collectAsState(
        initial = null,
    )

    val libraryState by
    mediaLibraryRepository
        .state
        .collectAsState()

    val scope =
        rememberCoroutineScope()

    var showMenu by
    remember {
        mutableStateOf(false)
    }

    var showRenameDialog by
    remember {
        mutableStateOf(false)
    }

    var showClearDialog by
    remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by
    remember {
        mutableStateOf(false)
    }

    var addToPlaylistMedia by
    remember {
        mutableStateOf<MediaItem?>(
            null,
        )
    }

    val playlist =
        detail

    if (playlist == null) {
        Column(
            modifier =
                modifier.fillMaxSize(),
        ) {
            NexPlayAppHeader(
                title = "Playlist",
                onBack = onBack,
            )

            NexPlayLoadingState(
                modifier =
                    Modifier.padding(
                        horizontal =
                            NexPlaySpacing
                                .screenPaddingX,
                    ),
            )
        }

        return
    }

    val mediaByUri =
        libraryState
            .items
            .associateBy(
                MediaItem::uri,
            )

    val resolvedItems =
        remember(
            playlist.items,
            libraryState.items,
        ) {
            playlist.items.map { entry ->
                entry to
                        mediaByUri[
                            entry.mediaUri
                        ]
            }
        }

    var displayedItems by
        remember(
            playlist.items,
            libraryState.items,
        ) {
            mutableStateOf(
                resolvedItems,
            )
        }

    val listState =
        rememberLazyListState()

    var draggingItemId by
        remember {
            mutableStateOf<String?>(
                null,
            )
        }

    var dragStartIndex by
        remember {
            mutableStateOf<Int?>(
                null,
            )
        }

    var dragCurrentIndex by
        remember {
            mutableStateOf<Int?>(
                null,
            )
        }

    var dragPointerY by
        remember {
            mutableFloatStateOf(
                0f,
            )
        }

    var dragGrabOffsetY by
        remember {
            mutableFloatStateOf(
                0f,
            )
        }

    var dragCardTopY by
        remember {
            mutableFloatStateOf(
                0f,
            )
        }

    var dragSlotTopY by
        remember {
            mutableFloatStateOf(
                0f,
            )
        }

    var dragRowHeightY by
        remember {
            mutableFloatStateOf(
                0f,
            )
        }

    var dragIsDropping by
        remember {
            mutableStateOf(
                false,
            )
        }

    var lastDragHopTimeMillis by
        remember {
            mutableLongStateOf(
                0L,
            )
        }

    val density =
        LocalDensity.current

    val reorderEdgePx =
        with(density) {
            72.dp.toPx()
        }

    val dragHandleHeightPx =
        with(density) {
            NexPlaySpacing
                .trailingIconBoxSize
                .toPx()
        }

    fun moveDisplayedItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (
            fromIndex !in
            displayedItems.indices ||
            toIndex !in
            displayedItems.indices ||
            fromIndex == toIndex
        ) {
            return
        }

        val nextItems =
            displayedItems
                .toMutableList()

        val movedItem =
            nextItems.removeAt(
                fromIndex,
            )

        nextItems.add(
            toIndex,
            movedItem,
        )

        displayedItems =
            nextItems
    }

    fun resetDragState() {
        draggingItemId =
            null
        dragStartIndex =
            null
        dragCurrentIndex =
            null
        dragPointerY =
            0f
        dragGrabOffsetY =
            0f
        dragCardTopY =
            0f
        dragSlotTopY =
            0f
        dragRowHeightY =
            0f
        dragIsDropping =
            false
        lastDragHopTimeMillis =
            0L
    }

    fun settleDrag(
        persist: Boolean,
    ) {
        val activeItemId =
            draggingItemId
                ?: return

        if (dragIsDropping) {
            return
        }

        val fromIndex =
            dragStartIndex
        val toIndex =
            dragCurrentIndex

        dragIsDropping =
            true

        scope.launch {
            for (attempt in 0 until 8) {
                withFrameNanos {
                    it
                }

                val liveInfo =
                    listState
                        .layoutInfo
                        .visibleItemsInfo
                        .firstOrNull {
                            it.key ==
                                    activeItemId
                        }

                val layoutSynced =
                    liveInfo == null ||
                            toIndex == null ||
                            liveInfo.index ==
                            toIndex

                if (
                    !listState
                        .isScrollInProgress &&
                    layoutSynced
                ) {
                    break
                }
            }

            val finalSlotTop =
                listState
                    .layoutInfo
                    .visibleItemsInfo
                    .firstOrNull {
                        it.key ==
                                activeItemId
                    }
                    ?.offset
                    ?.toFloat()

            if (
                finalSlotTop != null &&
                abs(
                    dragCardTopY -
                            finalSlotTop,
                ) > 1f
            ) {
                val startTop =
                    dragCardTopY

                val durationNanos =
                    130_000_000L

                val startNanos =
                    withFrameNanos {
                        it
                    }

                while (true) {
                    val frameNanos =
                        withFrameNanos {
                            it
                        }

                    val fraction =
                        (
                            (
                                frameNanos -
                                        startNanos
                            )
                                .toFloat() /
                                    durationNanos
                                        .toFloat()
                        )
                            .coerceIn(
                                0f,
                                1f,
                            )

                    dragCardTopY =
                        startTop +
                                (
                                    finalSlotTop -
                                            startTop
                                ) *
                                fraction

                    if (fraction >= 1f) {
                        break
                    }
                }
            }

            resetDragState()

            if (!persist) {
                displayedItems =
                    resolvedItems
                return@launch
            }

            if (
                fromIndex != null &&
                toIndex != null &&
                fromIndex !=
                toIndex
            ) {
                val reordered =
                    playlistRepository
                        .reorder(
                            playlistId =
                                playlist.id,
                            fromIndex =
                                fromIndex,
                            toIndex =
                                toIndex,
                        )

                if (!reordered) {
                    displayedItems =
                        resolvedItems
                }
            }
        }
    }

    LaunchedEffect(
        draggingItemId,
    ) {
        val activeItemId =
            draggingItemId
                ?: return@LaunchedEffect

        while (
            draggingItemId ==
            activeItemId &&
            !dragIsDropping
        ) {
            withFrameNanos {
                it
            }

            val currentIndex =
                dragCurrentIndex
                    ?: continue

            if (
                currentIndex !in
                displayedItems.indices ||
                dragRowHeightY <= 0f
            ) {
                continue
            }

            val layoutInfo =
                listState.layoutInfo

            val viewportTop =
                layoutInfo
                    .viewportStartOffset
                    .toFloat()

            val viewportBottom =
                layoutInfo
                    .viewportEndOffset
                    .toFloat()

            val maximumCardTop =
                (
                    viewportBottom -
                            dragRowHeightY
                )
                    .coerceAtLeast(
                        viewportTop,
                    )

            val desiredCardTop =
                dragPointerY -
                        dragGrabOffsetY

            val pushAboveEdge =
                (
                    viewportTop +
                            reorderEdgePx -
                            desiredCardTop
                )
                    .coerceAtLeast(
                        0f,
                    )

            val pushBelowEdge =
                (
                    desiredCardTop +
                            dragRowHeightY -
                            (
                                viewportBottom -
                                        reorderEdgePx
                            )
                )
                    .coerceAtLeast(
                        0f,
                    )

            val maxScrollPerFrame =
                dragRowHeightY *
                        0.10f

            val scrollDelta =
                when {
                    pushAboveEdge >
                        0f &&
                        listState
                            .canScrollBackward &&
                        currentIndex >
                        0 ->
                        -maxScrollPerFrame *
                                (
                                    pushAboveEdge /
                                            reorderEdgePx
                                )
                                    .coerceIn(
                                        0f,
                                        1f,
                                    )

                    pushBelowEdge >
                        0f &&
                        listState
                            .canScrollForward &&
                        currentIndex <
                        displayedItems
                            .lastIndex ->
                        maxScrollPerFrame *
                                (
                                    pushBelowEdge /
                                            reorderEdgePx
                                )
                                    .coerceIn(
                                        0f,
                                        1f,
                                    )

                    else ->
                        0f
                }

            if (scrollDelta != 0f) {
                listState.scrollBy(
                    scrollDelta,
                )
            }

            dragCardTopY =
                desiredCardTop
                    .coerceIn(
                        viewportTop,
                        maximumCardTop,
                    )

            val freshLayoutInfo =
                listState.layoutInfo

            val draggedItemInfo =
                freshLayoutInfo
                    .visibleItemsInfo
                    .firstOrNull {
                        it.key ==
                                activeItemId
                    }

            if (draggedItemInfo != null) {
                dragSlotTopY =
                    draggedItemInfo
                        .offset
                        .toFloat()
            }

            val layoutSynced =
                draggedItemInfo == null ||
                        draggedItemInfo.index ==
                        currentIndex

            if (!layoutSynced) {
                continue
            }

            val cardCenter =
                dragCardTopY +
                        dragRowHeightY /
                        2f

            val hoveredItem =
                freshLayoutInfo
                    .visibleItemsInfo
                    .firstOrNull {
                            itemInfo ->
                        cardCenter >=
                            itemInfo
                                .offset
                                .toFloat() &&
                                cardCenter <=
                                (
                                    itemInfo.offset +
                                            itemInfo.size
                                )
                                    .toFloat()
                    }

            val targetIndex =
                hoveredItem
                    ?.index

            if (
                targetIndex == null ||
                targetIndex ==
                currentIndex ||
                targetIndex !in
                displayedItems.indices
            ) {
                continue
            }

            val now =
                SystemClock
                    .elapsedRealtime()

            if (
                now -
                    lastDragHopTimeMillis <
                90L
            ) {
                continue
            }

            moveDisplayedItem(
                fromIndex =
                    currentIndex,
                toIndex =
                    targetIndex,
            )

            dragCurrentIndex =
                targetIndex

            lastDragHopTimeMillis =
                now
        }
    }

    val availableMedia =
        remember(
            displayedItems,
        ) {
            displayedItems.mapNotNull {
                    (_, media) ->
                media
            }
        }

    val playbackIndexByUri =
        remember(
            availableMedia,
        ) {
            availableMedia
                .withIndex()
                .associate {
                        indexedMedia ->
                    indexedMedia.value.uri to
                            indexedMedia.index
                }
        }

    Column(
        modifier =
            modifier.fillMaxSize(),
    ) {
        NexPlayAppHeader(
            title = playlist.name,
            subtitle =
                "${playlist.items.size} " +
                        if (
                            playlist.items.size ==
                            1
                        ) {
                            "track"
                        } else {
                            "tracks"
                        },
            onBack = onBack,
            actions = {
                Box {
                    NexPlayHeaderActionButton(
                        icon =
                            Icons.Filled
                                .MoreVert,
                        contentDescription =
                            "Playlist actions",
                        onClick = {
                            showMenu =
                                !showMenu
                        },
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text =
                                        "Rename",
                                )
                            },
                            onClick = {
                                showMenu = false
                                showRenameDialog =
                                    true
                            },
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text =
                                        "Clear",
                                )
                            },
                            enabled =
                                playlist.items
                                    .isNotEmpty(),
                            onClick = {
                                showMenu = false
                                showClearDialog =
                                    true
                            },
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text =
                                        "Delete",
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDeleteDialog =
                                    true
                            },
                        )
                    }
                }
            },
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start =
                            NexPlaySpacing
                                .screenPaddingX,
                        top =
                            NexPlaySpacing
                                .contentPaddingTop,
                        end =
                            NexPlaySpacing
                                .screenPaddingX,
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp,
                ),
        ) {
            Button(
                onClick = {
                    onPlayPlaylist(
                        playlist.id,
                        playlist.name,
                        availableMedia,
                        0,
                    )
                },
                enabled =
                    availableMedia
                        .isNotEmpty(),
                modifier =
                    Modifier.weight(1f),
                colors =
                    ButtonDefaults
                        .buttonColors(
                            contentColor =
                                Color.White,
                            disabledContentColor =
                                Color.White.copy(
                                    alpha = 0.38f,
                                ),
                        ),
            ) {
                Icon(
                    imageVector =
                        Icons.Filled
                            .PlayArrow,
                    contentDescription =
                        null,
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp,
                        ),
                )

                Text(
                    text = "Play All",
                )
            }

            OutlinedButton(
                onClick = {
                    val shuffled =
                        availableMedia
                            .shuffled()

                    onPlayPlaylist(
                        playlist.id,
                        playlist.name,
                        shuffled,
                        0,
                    )
                },
                enabled =
                    availableMedia.size > 1,
                modifier =
                    Modifier.weight(1f),
            ) {
                Icon(
                    imageVector =
                        Icons.Filled
                            .Shuffle,
                    contentDescription =
                        null,
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp,
                        ),
                )

                Text(
                    text = "Shuffle",
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    10.dp,
                ),
        )

        if (playlist.items.isEmpty()) {
            NexPlayEmptyState(
                icon =
                    Icons.AutoMirrored
                        .Outlined
                        .QueueMusic,
                title =
                    "No tracks yet",
                subtitle =
                    "Add media to this playlist first.",
                modifier =
                    Modifier.padding(
                        horizontal =
                            NexPlaySpacing
                                .screenPaddingX,
                    ),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                    PaddingValues(
                        bottom =
                            NexPlaySpacing
                                .contentPaddingBottom,
                    ),
            ) {
                itemsIndexed(
                    items =
                        displayedItems,
                    key = {
                            _,
                            pair ->
                        pair.first.id
                    },
                ) {
                        index,
                        pair ->
                    val entry =
                        pair.first
                    val media =
                        pair.second

                    val currentIndex =
                        playbackIndexByUri[
                            entry.mediaUri
                        ] ?: -1

                    val isDragging =
                        draggingItemId ==
                                entry.id

                    PlaylistMediaRow(
                        media = media,
                        mediaUri =
                            entry.mediaUri,
                        modifier =
                            if (isDragging) {
                                Modifier
                                    .zIndex(
                                        1f,
                                    )
                                    .graphicsLayer {
                                        val liveSlotTop =
                                            listState
                                                .layoutInfo
                                                .visibleItemsInfo
                                                .firstOrNull {
                                                    it.key ==
                                                            entry.id
                                                }
                                                ?.offset
                                                ?.toFloat()

                                        translationY =
                                            dragCardTopY -
                                                    (
                                                        liveSlotTop
                                                            ?: dragSlotTopY
                                                    )
                                    }
                            } else {
                                Modifier
                            },
                        onClick = {
                            if (
                                currentIndex >= 0
                            ) {
                                onPlayPlaylist(
                                    playlist.id,
                                    playlist.name,
                                    availableMedia,
                                    currentIndex,
                                )
                            }
                        },
                        onDragStart = {
                                handleTouchY ->
                            val draggedInfo =
                                listState
                                    .layoutInfo
                                    .visibleItemsInfo
                                    .firstOrNull {
                                        it.key ==
                                                entry.id
                                    }

                            if (draggedInfo != null) {
                                val rowHeight =
                                    draggedInfo
                                        .size
                                        .toFloat()

                                val handleTopInRow =
                                    (
                                        rowHeight -
                                                dragHandleHeightPx
                                    ) /
                                            2f

                                val grabOffset =
                                    (
                                        handleTopInRow +
                                                handleTouchY
                                    )
                                        .coerceIn(
                                            0f,
                                            rowHeight,
                                        )

                                draggingItemId =
                                    entry.id
                                dragStartIndex =
                                    index
                                dragCurrentIndex =
                                    index
                                dragRowHeightY =
                                    rowHeight
                                dragGrabOffsetY =
                                    grabOffset
                                dragPointerY =
                                    draggedInfo
                                        .offset
                                        .toFloat() +
                                            grabOffset
                                dragCardTopY =
                                    draggedInfo
                                        .offset
                                        .toFloat()
                                dragSlotTopY =
                                    draggedInfo
                                        .offset
                                        .toFloat()
                                dragIsDropping =
                                    false
                                lastDragHopTimeMillis =
                                    0L
                            }
                        },
                        onDrag = { deltaY ->
                            if (
                                draggingItemId ==
                                entry.id &&
                                !dragIsDropping
                            ) {
                                dragPointerY +=
                                    deltaY
                            }
                        },
                        onDragEnd = {
                            settleDrag(
                                persist = true,
                            )
                        },
                        onDragCancel = {
                            settleDrag(
                                persist = false,
                            )
                        },
                        onAddToPlaylist = {
                            if (media != null) {
                                addToPlaylistMedia =
                                    media
                            }
                        },
                        onRemove = {
                            scope.launch {
                                playlistRepository
                                    .removeItem(
                                        playlistId =
                                            playlist.id,
                                        playlistItemId =
                                            entry.id,
                                    )
                            }
                        },
                    )
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

    addToPlaylistMedia?.let { media ->
        AddToPlaylistSheet(
            playlistRepository =
                playlistRepository,
            mediaItems =
                listOf(
                    media,
                ),
            onDismiss = {
                addToPlaylistMedia =
                    null
            },
        )
    }

    if (showRenameDialog) {
        PlaylistNameDialog(
            title = "Rename Playlist",
            initialName =
                playlist.name,
            confirmLabel = "Rename",
            allowBlankAsDefault =
                false,
            onDismiss = {
                showRenameDialog =
                    false
            },
            onConfirm = { name ->
                scope.launch {
                    playlistRepository
                        .renamePlaylist(
                            playlistId =
                                playlist.id,
                            name = name,
                        )

                    showRenameDialog =
                        false
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },
            title = {
                Text(
                    text =
                        "Clear playlist?",
                )
            },
            text = {
                Text(
                    text =
                        "All playlist items will be removed. The playlist itself will remain.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            playlistRepository
                                .clearPlaylist(
                                    playlist.id,
                                )

                            showClearDialog =
                                false
                        }
                    },
                ) {
                    Text(
                        text = "Clear",
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                    },
                ) {
                    Text(
                        text = "Cancel",
                    )
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(
                    text =
                        "Delete playlist?",
                )
            },
            text = {
                Text(
                    text =
                        "\"${playlist.name}\" will be permanently deleted.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (
                                playlistRepository
                                    .deletePlaylist(
                                        playlist.id,
                                    )
                            ) {
                                onBack()
                            } else {
                                showDeleteDialog =
                                    false
                            }
                        }
                    },
                ) {
                    Text(
                        text = "Delete",
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    },
                ) {
                    Text(
                        text = "Cancel",
                    )
                }
            },
        )
    }
}

@Composable
private fun PlaylistMediaRow(
    media: MediaItem?,
    mediaUri: String,
    onClick: () -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRowMenu by
        remember {
            mutableStateOf(false)
        }

    val currentOnDragStart =
        rememberUpdatedState(
            onDragStart,
        )

    val currentOnDrag =
        rememberUpdatedState(
            onDrag,
        )

    val currentOnDragEnd =
        rememberUpdatedState(
            onDragEnd,
        )

    val currentOnDragCancel =
        rememberUpdatedState(
            onDragCancel,
        )

    val title =
        media
            ?.title
            ?.trim()
            ?.takeIf(
                String::isNotEmpty,
            )
            ?: media
                ?.displayName
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )
            ?: mediaUri
                .substringAfterLast(
                    '/',
                )
                .ifBlank {
                    "Unavailable media"
                }

    val subtitle =
        when (media) {
            is AudioItem ->
                media.artist
                    ?.trim()
                    ?.takeIf(
                        String::isNotEmpty,
                    )
                    ?: "Unknown Artist"

            is VideoItem ->
                "Video"

            null ->
                "Unavailable on this device"
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        8.dp,
                    ),
                )
                .clickable(
                    enabled =
                        media != null,
                    onClick = onClick,
                )
                .padding(
                    horizontal =
                        NexPlaySpacing.screenPaddingX,
                    vertical =
                        NexPlaySpacing
                            .rowVerticalPadding,
                ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        if (media == null) {
            Surface(
                modifier =
                    Modifier.size(
                        NexPlaySpacing
                            .listLeadingSize,
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
                        imageVector =
                            Icons.Outlined
                                .ErrorOutline,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                NexPlaySpacing
                                    .listLeadingIconSize,
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .error,
                    )
                }
            }
        } else {
            MediaThumbnailBox(
                item = media,
                size =
                    NexPlaySpacing
                        .listLeadingSize,
                fallbackIcon =
                    when (media) {
                        is VideoItem ->
                            Icons.Filled
                                .Videocam

                        is AudioItem ->
                            Icons.Filled
                                .MusicNote
                    },
            )
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
                    if (media == null) {
                        MaterialTheme
                            .colorScheme
                            .error
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
            )
        }

        Box(
            modifier =
                Modifier
                    .size(
                        NexPlaySpacing
                            .trailingIconBoxSize,
                    )
                    .pointerInput(
                        mediaUri,
                    ) {
                        detectDragGestures(
                            onDragStart = {
                                    offset ->
                                currentOnDragStart
                                    .value(
                                        offset.y,
                                    )
                            },
                            onDragEnd = {
                                currentOnDragEnd
                                    .value()
                            },
                            onDragCancel = {
                                currentOnDragCancel
                                    .value()
                            },
                            onDrag = {
                                    change,
                                    dragAmount ->
                                change.consume()

                                currentOnDrag
                                    .value(
                                        dragAmount.y,
                                    )
                            },
                        )
                    },
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                imageVector =
                    Icons.Filled
                        .DragHandle,
                contentDescription =
                    "Reorder",
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

        Box(
            modifier =
                Modifier
                    .size(
                        NexPlaySpacing
                            .headerIconButtonSize,
                    )
                    .offset(
                        x =
                            NexPlaySpacing
                                .rightEdgeIconOffsetX,
                    ),
        ) {
            NexPlayHeaderActionButton(
                icon =
                    Icons.Filled
                        .MoreVert,
                contentDescription =
                    "Media actions",
                onClick = {
                    showRowMenu =
                        !showRowMenu
                },
            )

            DropdownMenu(
                expanded = showRowMenu,
                onDismissRequest = {
                    showRowMenu =
                        false
                },
                modifier =
                    Modifier
                        .width(
                            224.dp,
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
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                        )
                    },
                    onClick = {
                        showRowMenu =
                            false
                        onRemove()
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                "Add to Playlist",
                        )
                    },
                    enabled =
                        media != null,
                    onClick = {
                        showRowMenu =
                            false
                        onAddToPlaylist()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToPlaylistSheet(
    playlistRepository:
        PlaylistRepository,
    mediaItems:
        List<MediaItem>,
    onDismiss: () -> Unit,
) {
    if (mediaItems.isEmpty()) {
        return
    }

    val playlists by
        playlistRepository
            .playlists
            .collectAsState(
                initial = emptyList(),
            )

    val scope =
        rememberCoroutineScope()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded =
                true,
        )

    val listState =
        rememberLazyListState()

    var playlistName by
        remember {
            mutableStateOf(
                "New Playlist",
            )
        }

    ModalBottomSheet(
        onDismissRequest =
            onDismiss,
        sheetState =
            sheetState,
        containerColor =
            MaterialTheme
                .colorScheme
                .surface,
        tonalElevation = 0.dp,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(
                        0.9f,
                    )
                    .padding(
                        start = 20.dp,
                        top = 16.dp,
                        end = 20.dp,
                        bottom = 20.dp,
                    ),
        ) {
            Text(
                text =
                    "Add to Playlist",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
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
                text =
                    if (
                        mediaItems.size ==
                        1
                    ) {
                        mediaItems
                            .first()
                            .title
                            .trim()
                            .ifEmpty {
                                mediaItems
                                    .first()
                                    .displayName
                            }
                    } else {
                        "${mediaItems.size} selected media"
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

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp,
                    ),
            )

            OutlinedTextField(
                value = playlistName,
                onValueChange = {
                    playlistName = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        text =
                            "Playlist name",
                    )
                },
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp,
                    ),
            )

            Button(
                onClick = {
                    scope.launch {
                        playlistRepository
                            .createPlaylist(
                                name =
                                    playlistName,
                                initialMediaUris =
                                    mediaItems.map(
                                        MediaItem::uri,
                                    ),
                            )

                        onDismiss()
                    }
                },
                modifier =
                    Modifier.fillMaxWidth(),
            ) {
                Text(
                    text =
                        "Create playlist",
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp,
                    ),
            )

            Text(
                text =
                    "Existing playlists",
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
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

            if (playlists.isEmpty()) {
                Text(
                    text =
                        "No playlists yet. Create one to add selected media.",
                    modifier =
                        Modifier.padding(
                            vertical =
                                12.dp,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )
            } else {
                Box(
                    modifier =
                        Modifier.weight(1f),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier.fillMaxSize(),
                    ) {
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
                                    scope.launch {
                                        playlistRepository
                                            .addMedia(
                                                playlistId =
                                                    playlist.id,
                                                mediaUris =
                                                    mediaItems.map(
                                                        MediaItem::uri,
                                                    ),
                                            )

                                        onDismiss()
                                    }
                                },
                            )
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
    }
}

@Composable
internal fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    allowBlankAsDefault: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by
    remember(
        initialName,
    ) {
        mutableStateOf(
            initialName,
        )
    }

    val canConfirm =
        allowBlankAsDefault ||
                name.trim()
                    .isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        text =
                            "Playlist name",
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                    )
                },
                enabled = canConfirm,
            ) {
                Text(
                    text =
                        confirmLabel,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "Cancel",
                )
            }
        },
    )
}
