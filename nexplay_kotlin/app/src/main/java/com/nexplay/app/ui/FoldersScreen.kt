package com.nexplay.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nexplay.app.data.media.AudioMediaPermission
import com.nexplay.app.data.media.MediaLibraryRepository
import com.nexplay.app.data.media.MediaLibrarySourceStatus
import com.nexplay.app.data.media.MediaLibraryState
import com.nexplay.app.data.media.VideoMediaPermission
import com.nexplay.app.data.playlist.PlaylistRepository
import com.nexplay.app.data.preferences.NexPlayPreferences
import com.nexplay.app.data.preferences.NexPlayPreferencesRepository
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.ui.theme.NexPlayThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    repository: MediaLibraryRepository,
    preferencesRepository:
        NexPlayPreferencesRepository,
    onOpenSearch: () -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    themeMode: NexPlayThemeMode,
    onThemeModeSelected:
        (NexPlayThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState =
        rememberLazyListState()
    val context = LocalContext.current
    val activity =
        remember(context) {
            context.findActivity()
        }

    val requiredMediaPermissions =
        remember {
            listOf(
                AudioMediaPermission.requiredPermission(
                    Build.VERSION.SDK_INT,
                ),
                VideoMediaPermission.requiredPermission(
                    Build.VERSION.SDK_INT,
                ),
            )
                .distinct()
        }

    val preferences by
        preferencesRepository
            .preferences
            .collectAsState(
                initial =
                    NexPlayPreferences(),
            )

    val sortConfig =
        preferences
            .resolveFolderSortConfig()

    var isPullRefreshing by remember {
        mutableStateOf(false)
    }

    var hasRequestedMediaPermissions by remember {
        mutableStateOf(false)
    }

    var requiresMediaSettings by remember {
        mutableStateOf(false)
    }

    var showAboutDialog by
        remember {
            mutableStateOf(false)
        }

    val folders =
        sortFolderItems(
            items =
                buildFolderListItems(
                    folders = repository.getFolders(),
                    mediaItems = state.items,
                ),
            config = sortConfig,
        )

    fun missingMediaPermissions(): List<String> {
        return requiredMediaPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshLibrary() {
        scope.launch {
            isPullRefreshing = true

            try {
                withContext(Dispatchers.IO) {
                    repository.refresh()
                }
            } finally {
                isPullRefreshing = false
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            val deniedPermissions =
                missingMediaPermissions()

            requiresMediaSettings =
                deniedPermissions.isNotEmpty() &&
                        activity != null &&
                        deniedPermissions.all { permission ->
                            !ActivityCompat
                                .shouldShowRequestPermissionRationale(
                                    activity,
                                    permission,
                                )
                        }

            refreshLibrary()
        }

    fun requestMediaPermissions() {
        val missingPermissions =
            missingMediaPermissions()

        if (missingPermissions.isEmpty()) {
            requiresMediaSettings = false
            refreshLibrary()
            return
        }

        hasRequestedMediaPermissions = true

        permissionLauncher.launch(
            missingPermissions.toTypedArray(),
        )
    }

    fun openMediaSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            ).apply {
                data =
                    Uri.fromParts(
                        "package",
                        context.packageName,
                        null,
                    )
            },
        )
    }

    LaunchedEffect(
        repository,
        activity,
    ) {
        val currentState =
            repository.state.value

        val requiresInitialScan =
            currentState.audioStatus ==
                    MediaLibrarySourceStatus.NOT_SCANNED ||
                    currentState.videoStatus ==
                    MediaLibrarySourceStatus.NOT_SCANNED

        if (!requiresInitialScan) {
            return@LaunchedEffect
        }

        val missingPermissions =
            missingMediaPermissions()

        if (
            missingPermissions.isNotEmpty() &&
            activity != null &&
            !hasRequestedMediaPermissions
        ) {
            hasRequestedMediaPermissions = true

            permissionLauncher.launch(
                missingPermissions.toTypedArray(),
            )
        } else {
            withContext(Dispatchers.IO) {
                repository.refresh()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        NexPlayAppHeader(
            title = "NexPlay",
            subtitle = "Local-first Media Player",
            isPrimaryScreen = true,
            actions = {
                NexPlayHeaderActionButton(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search",
                    onClick = onOpenSearch,
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            NexPlaySpacing.headerActionGap,
                        ),
                )

                FolderMoreMenu(
                    config = sortConfig,
                    themeMode = themeMode,
                    onThemeModeSelected =
                        onThemeModeSelected,
                    onOpenAbout = {
                        showAboutDialog =
                            true
                    },
                    onSelected = { field ->
                        val nextConfig =
                            sortConfig.toggle(
                                field,
                            )

                        scope.launch {
                            preferencesRepository
                                .setFolderSort(
                                    fieldName =
                                        nextConfig
                                            .field
                                            .name,
                                    directionName =
                                        nextConfig
                                            .direction
                                            .name,
                                )
                        }
                    },
                )
            },
        )

        PullToRefreshBox(
            isRefreshing = isPullRefreshing,
            onRefresh = ::refreshLibrary,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top =
                            NexPlaySpacing
                                .contentPaddingTop,
                        bottom =
                            NexPlaySpacing
                                .contentPaddingBottom +
                                NexPlayBottomBarHeight,
                    ),
            ) {
                when {
                    state.isRefreshing &&
                            folders.isEmpty() -> {
                        item {
                            NexPlayLoadingState()
                        }
                    }

                    folders.isEmpty() &&
                            hasPermissionDenied(state) -> {
                        item {
                            NexPlayEmptyState(
                                icon = Icons.Outlined.Lock,
                                title =
                                    "Media access is not allowed",
                                subtitle =
                                    if (requiresMediaSettings) {
                                        "Media access is blocked. Open system settings to allow audio and video access, then pull to refresh."
                                    } else {
                                        "Allow audio and video access to show your media library."
                                    },
                                actionLabel =
                                    if (requiresMediaSettings) {
                                        "Open settings"
                                    } else {
                                        "Allow access"
                                    },
                                onAction =
                                    if (requiresMediaSettings) {
                                        ::openMediaSettings
                                    } else {
                                        ::requestMediaPermissions
                                    },
                            )
                        }
                    }

                    folders.isEmpty() &&
                            hasScanFailure(state) -> {
                        item {
                            NexPlayEmptyState(
                                icon =
                                    Icons.Outlined.ErrorOutline,
                                title =
                                    "Media scan failed",
                                subtitle =
                                    "Pull to refresh to try scanning the media library again.",
                            )
                        }
                    }

                    folders.isEmpty() -> {
                        item {
                            NexPlayEmptyState(
                                icon =
                                    Icons.Outlined.FolderOpen,
                                title =
                                    "No media folders found yet",
                                subtitle =
                                    "Pull to refresh after granting media access.",
                            )
                        }
                    }

                    else -> {
                        items(
                            items = folders,
                            key = { item ->
                                folderKey(item.folder)
                            },
                        ) { item ->
                            FolderRow(
                                item = item,
                                onClick = {
                                    onOpenFolder(
                                        item.folder,
                                    )
                                },
                            )
                        }
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

    if (showAboutDialog) {
        NexPlayAboutDialog(
            onDismiss = {
                showAboutDialog =
                    false
            },
        )
    }
}

internal val FolderDetailSelectionActionBarHeight =
    64.dp

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun FolderDetailScreen(
    repository: MediaLibraryRepository,
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
    val state by repository.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState =
        rememberLazyListState()

    val preferences by
        preferencesRepository
            .preferences
            .collectAsState(
                initial =
                    NexPlayPreferences(),
            )

    val sortConfig =
        preferences
            .resolveMediaSortConfig()

    var isPullRefreshing by remember {
        mutableStateOf(false)
    }

    var selectedMediaUris by
        remember(folder) {
            mutableStateOf<Set<String>>(
                emptySet(),
            )
        }

    var isAddToPlaylistOpen by
        remember {
            mutableStateOf(false)
        }

    val folderItems =
        buildFolderListItems(
            folders = repository.getFolders(),
            mediaItems = state.items,
        )

    val resolvedFolder =
        folder?.let { selectedFolder ->
            folderItems.firstOrNull { item ->
                folderKey(item.folder) ==
                        folderKey(selectedFolder)
            }
        }

    val mediaItems =
        sortMediaItems(
            items =
                resolvedFolder
                    ?.folder
                    ?.let(
                        repository::getMediaInFolder,
                    )
                    .orEmpty(),
            config = sortConfig,
        )

    val isSelectionMode =
        selectedMediaUris.isNotEmpty()

    val selectedItems =
        mediaItems.filter { item ->
            item.uri in
                selectedMediaUris
        }

    val areAllItemsSelected =
        mediaItems.isNotEmpty() &&
            selectedItems.size ==
            mediaItems.size

    fun updateSelection(
        nextSelection:
            Set<String>,
    ) {
        selectedMediaUris =
            nextSelection

        onSelectionModeChanged(
            nextSelection
                .isNotEmpty(),
        )
    }

    fun toggleSelection(
        item: MediaItem,
    ) {
        val nextSelection =
            selectedMediaUris
                .toMutableSet()

        if (
            !nextSelection.add(
                item.uri,
            )
        ) {
            nextSelection.remove(
                item.uri,
            )
        }

        updateSelection(
            nextSelection,
        )
    }

    fun selectAll() {
        updateSelection(
            mediaItems
                .map(
                    MediaItem::uri,
                )
                .toSet(),
        )
    }

    fun clearSelection() {
        updateSelection(
            emptySet(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onSelectionModeChanged(
                false,
            )
        }
    }

    BackHandler(
        enabled = isSelectionMode,
    ) {
        clearSelection()
    }

    fun refreshLibrary() {
        scope.launch {
            isPullRefreshing = true

            try {
                withContext(Dispatchers.IO) {
                    repository.refresh()
                }
            } finally {
                isPullRefreshing = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        NexPlayAppHeader(
            title =
                resolvedFolder
                    ?.folder
                    ?.name
                    ?: "Folder not found",
            subtitle =
                resolvedFolder
                    ?.let(::formatFolderSubtitle)
                    ?: "Pull to refresh",
            onBack =
                if (isSelectionMode) {
                    ::clearSelection
                } else {
                    onBack
                },
            actions = {
                if (isSelectionMode) {
                    NexPlayHeaderActionButton(
                        icon =
                            if (
                                areAllItemsSelected
                            ) {
                                Icons.Filled
                                    .CheckCircle
                            } else {
                                Icons.Outlined
                                    .SelectAll
                            },
                        contentDescription =
                            if (
                                areAllItemsSelected
                            ) {
                                "Deselect all"
                            } else {
                                "Select all"
                            },
                        onClick =
                            if (
                                areAllItemsSelected
                            ) {
                                ::clearSelection
                            } else {
                                ::selectAll
                            },
                    )
                } else {
                    MediaSortMenu(
                        config = sortConfig,
                        onSelected = { field ->
                            val nextConfig =
                                sortConfig.toggle(
                                    field,
                                )

                            scope.launch {
                                preferencesRepository
                                    .setMediaSort(
                                        fieldName =
                                            nextConfig
                                                .field
                                                .name,
                                        directionName =
                                            nextConfig
                                                .direction
                                                .name,
                                    )
                            }
                        },
                    )
                }
            },
        )

        Box(
            modifier =
                Modifier.weight(1f),
        ) {
            PullToRefreshBox(
                isRefreshing =
                    isPullRefreshing,
                onRefresh =
                    ::refreshLibrary,
                modifier =
                    Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top =
                                NexPlaySpacing
                                    .contentPaddingTop,
                            bottom =
                                NexPlaySpacing
                                    .contentPaddingBottom +
                                    if (
                                        isSelectionMode
                                    ) {
                                        FolderDetailSelectionActionBarHeight
                                    } else {
                                        0.dp
                                    },
                        ),
                ) {
                when {
                    state.isRefreshing &&
                            mediaItems.isEmpty() -> {
                        item {
                            NexPlayLoadingState()
                        }
                    }

                    resolvedFolder == null -> {
                        item {
                            NexPlayEmptyState(
                                icon =
                                    Icons.Outlined.FolderOff,
                                title =
                                    "Folder is no longer available",
                                subtitle =
                                    "Pull to refresh the media library.",
                            )
                        }
                    }

                    mediaItems.isEmpty() -> {
                        item {
                            NexPlayEmptyState(
                                icon =
                                    Icons.Outlined.PermMedia,
                                title =
                                    "No media in this folder",
                                subtitle =
                                    "Pull to refresh if files were recently added.",
                            )
                        }
                    }

                    else -> {
                        items(
                            items = mediaItems,
                            key = MediaItem::uri,
                        ) { item ->
                            MediaRow(
                                item = item,
                                isCurrent =
                                    item.uri ==
                                        currentMediaUri,
                                isSelectionMode =
                                    isSelectionMode,
                                isSelected =
                                    item.uri in
                                        selectedMediaUris,
                                onClick = {
                                    if (
                                        isSelectionMode
                                    ) {
                                        toggleSelection(
                                            item,
                                        )
                                    } else {
                                        when (item) {
                                            is AudioItem ->
                                                onPlayAudio(
                                                    item,
                                                    mediaItems,
                                                )

                                            is VideoItem ->
                                                onPlayVideo(
                                                    item,
                                                    mediaItems,
                                                )
                                        }
                                    }
                                },
                                onLongClick = {
                                    toggleSelection(
                                        item,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            NexPlayScrollbar(
                listState = listState,
                modifier =
                    Modifier
                        .align(
                            Alignment.CenterEnd,
                        )
                        .padding(
                            bottom =
                                if (
                                    isSelectionMode
                                ) {
                                    FolderDetailSelectionActionBarHeight
                                } else {
                                    0.dp
                                },
                        ),
            )
        }

            if (isSelectionMode) {
                FolderDetailSelectionActionBar(
                    selectedCount =
                        selectedItems.size,
                    onAdd = {
                        if (
                            selectedItems
                                .isNotEmpty()
                        ) {
                            isAddToPlaylistOpen =
                                true
                        }
                    },
                    onClear =
                        ::clearSelection,
                    modifier =
                        Modifier.align(
                            Alignment.BottomCenter,
                        ),
                )
            }
        }
    }

    if (isAddToPlaylistOpen) {
        AddToPlaylistSheet(
            playlistRepository =
                playlistRepository,
            mediaItems =
                selectedItems,
            onDismiss = {
                isAddToPlaylistOpen =
                    false
                clearSelection()
            },
        )
    }
}

@Composable
private fun FolderDetailSelectionActionBar(
    selectedCount: Int,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    FolderDetailSelectionActionBarHeight,
                ),
        color =
            MaterialTheme
                .colorScheme
                .background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        NexPlaySpacing
                            .screenPaddingX,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                text =
                    "$selectedCount selected",
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
                        .onBackground,
            )

            TextButton(
                onClick = onAdd,
                enabled =
                    selectedCount > 0,
            ) {
                Icon(
                    imageVector =
                        Icons.Filled
                            .PlaylistAdd,
                    contentDescription =
                        null,
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp,
                        ),
                )

                Text(
                    text = "Add",
                )
            }

            IconButton(
                onClick = onClear,
            ) {
                Icon(
                    imageVector =
                        Icons.Filled.Close,
                    contentDescription =
                        "Clear selection",
                )
            }
        }
    }
}

@Composable
private fun FolderMoreMenu(
    config: SortConfig<FolderSortField>,
    themeMode: NexPlayThemeMode,
    onThemeModeSelected:
        (NexPlayThemeMode) -> Unit,
    onOpenAbout: () -> Unit,
    onSelected: (FolderSortField) -> Unit,
) {
    var expanded by remember {
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
            Spacer(
                modifier =
                    Modifier.height(
                        8.dp,
                    ),
            )

            FolderSortField.entries.forEach { field ->
                SortMenuItem(
                    label =
                        folderSortLabel(
                            field,
                        ),
                    isActive =
                        config.field ==
                                field,
                    direction =
                        config.direction,
                    onClick = {
                        onSelected(field)
                        expanded = false
                    },
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp,
                    ),
            )

            val themeOptions =
                listOf(
                    Triple(
                        NexPlayThemeMode.SYSTEM,
                        Icons.Outlined.BrightnessAuto,
                        "System theme",
                    ),
                    Triple(
                        NexPlayThemeMode.DARK,
                        Icons.Outlined.DarkMode,
                        "Dark theme",
                    ),
                    Triple(
                        NexPlayThemeMode.LIGHT,
                        Icons.Outlined.LightMode,
                        "Light theme",
                    ),
                )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 18.dp,
                            vertical = 6.dp,
                        ),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
            ) {
                themeOptions.forEach {
                        (
                            mode,
                            icon,
                            description,
                        ) ->
                    val isSelected =
                        themeMode == mode

                    Surface(
                        modifier =
                            Modifier.size(
                                48.dp,
                            ),
                        shape =
                            RoundedCornerShape(
                                12.dp,
                            ),
                        color =
                            if (isSelected) {
                                MaterialTheme
                                    .colorScheme
                                    .primary
                            } else {
                                Color.Transparent
                            },
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        IconButton(
                            onClick = {
                                onThemeModeSelected(
                                    mode,
                                )
                                expanded = false
                            },
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription =
                                    description,
                                tint =
                                    if (isSelected) {
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp,
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
private fun MediaSortMenu(
    config: SortConfig<MediaSortField>,
    onSelected: (MediaSortField) -> Unit,
) {
    var expanded by remember {
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
            modifier = Modifier
                .width(208.dp)
                .clip(
                    RoundedCornerShape(18.dp),
                ),
            offset = DpOffset(
                x = (-40).dp,
                y = 0.dp,
            ),
            containerColor =
                MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            MediaSortField.entries.forEach { field ->
                SortMenuItem(
                    label = mediaSortLabel(field),
                    isActive =
                        config.field == field,
                    direction =
                        config.direction,
                    onClick = {
                        onSelected(field)
                        expanded = false
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    isActive: Boolean,
    direction: SortDirection,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
            )
        },
        trailingIcon = {
            if (isActive) {
                Icon(
                    imageVector =
                        if (
                            direction ==
                            SortDirection.ASCENDING
                        ) {
                            Icons.Filled.ArrowUpward
                        } else {
                            Icons.Filled.ArrowDownward
                        },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        onClick = onClick,
        modifier = Modifier
            .height(52.dp)
            .clip(
                RoundedCornerShape(12.dp),
            ),
        contentPadding =
            PaddingValues(
                horizontal = 18.dp,
            ),
    )
}

@Composable
private fun FolderRow(
    item: FolderListItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = NexPlaySpacing.listRowMinHeight,
            )
            .clip(
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal =
                    NexPlaySpacing.screenPaddingX,
                vertical =
                    NexPlaySpacing.rowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(
                    NexPlaySpacing.listLeadingSize,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(
                            NexPlaySpacing.folderIconSize,
                        )
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                        },
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }

        Spacer(
            modifier =
                Modifier.width(
                    NexPlaySpacing.itemGap,
                ),
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.folder.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                        NexPlaySpacing.listTextGap,
                    ),
            )

            Text(
                text =
                    formatFolderSubtitle(item),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier =
                Modifier.size(
                    NexPlaySpacing
                        .trailingIconBoxSize,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector =
                    Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(
                            NexPlaySpacing
                                .trailingIconSize,
                        )
                        .offset(
                            x =
                                NexPlaySpacing
                                    .rightEdgeIconOffsetX,
                        ),
                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
)
@Composable
private fun MediaRow(
    item: MediaItem,
    isCurrent: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        NexPlaySpacing
                            .listRowMinHeight,
                )
                .clip(
                    RoundedCornerShape(
                        8.dp,
                    ),
                )
                .background(
                    if (isSelected) {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    } else {
                        Color.Transparent
                    },
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick =
                        onLongClick,
                )
                .padding(
                    horizontal =
                        NexPlaySpacing
                            .screenPaddingX,
                    vertical =
                        NexPlaySpacing
                            .rowVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaThumbnailBox(
            item = item,
            size =
                NexPlaySpacing
                    .listLeadingSize,
            fallbackIcon =
                when (item) {
                    is AudioItem ->
                        Icons.Filled.MusicNote

                    is VideoItem ->
                        Icons.Rounded.PlayArrow
                },
        )

        Spacer(
            modifier =
                Modifier.width(
                    NexPlaySpacing.itemGap,
                ),
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.title,
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
                        .titleMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground,
            )

            Spacer(
                modifier =
                    Modifier.height(
                        NexPlaySpacing.listTextGap,
                    ),
            )

            Text(
                text = formatMediaSubtitle(item),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier =
                Modifier.width(36.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text =
                    formatMediaDuration(
                        item.durationMs,
                    ).orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.End,
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
                    if (isSelectionMode) {
                        if (isSelected) {
                            Icons.Filled
                                .CheckCircle
                        } else {
                            Icons.Outlined
                                .RadioButtonUnchecked
                        }
                    } else {
                        Icons.Filled
                            .MoreVert
                    },
                contentDescription =
                    if (isSelectionMode) {
                        if (isSelected) {
                            "Selected"
                        } else {
                            "Not selected"
                        }
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .size(
                            NexPlaySpacing
                                .trailingIconSize,
                        )
                        .offset(
                            x =
                                NexPlaySpacing
                                    .rightEdgeIconOffsetX,
                        ),
                tint =
                    if (
                        isSelectionMode &&
                        isSelected
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onBackground
                    },
            )
        }
    }
}

private tailrec fun Context.findActivity():
        Activity? {
    return when (this) {
        is Activity ->
            this

        is ContextWrapper ->
            baseContext.findActivity()

        else ->
            null
    }
}

private fun hasPermissionDenied(
    state: MediaLibraryState,
): Boolean {
    return state.audioStatus ==
            MediaLibrarySourceStatus.PERMISSION_DENIED ||
            state.videoStatus ==
            MediaLibrarySourceStatus.PERMISSION_DENIED
}

private fun hasScanFailure(
    state: MediaLibraryState,
): Boolean {
    return state.audioStatus ==
            MediaLibrarySourceStatus.FAILED ||
            state.videoStatus ==
            MediaLibrarySourceStatus.FAILED
}

private fun formatFolderSubtitle(
    item: FolderListItem,
): String {
    return "${item.audioCount} audios \u2022 ${item.videoCount} videos"
}

private fun formatMediaSubtitle(
    item: MediaItem,
): String {
    return when (item) {
        is AudioItem ->
            item.artist
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: item.folder
                    ?.name
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                ?: "Unknown Artist"

        is VideoItem ->
            item.folder
                ?.name
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: "Unknown Folder"
    }
}

private fun formatMediaDuration(
    durationMs: Long?,
): String? {
    val resolvedDuration =
        durationMs
            ?.takeIf { duration ->
                duration > 0L
            }
            ?: return null

    val totalSeconds =
        resolvedDuration / 1_000L

    val hours =
        totalSeconds / 3_600L

    val minutes =
        (totalSeconds % 3_600L) / 60L

    val seconds =
        totalSeconds % 60L

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