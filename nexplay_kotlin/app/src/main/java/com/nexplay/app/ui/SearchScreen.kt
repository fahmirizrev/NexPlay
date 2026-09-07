package com.nexplay.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexplay.app.data.media.MediaLibraryRepository
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem

@Composable
internal fun SearchScreen(
    repository: MediaLibraryRepository,
    onBack: () -> Unit,
    onPlayResult: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by repository.state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState =
        rememberLazyListState()

    var query by rememberSaveable {
        mutableStateOf("")
    }

    val results =
        filterSearchResults(
            items = state.items,
            query = query,
        )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NexPlaySpacing.screenPaddingX,
                        top = NexPlaySpacing.headerPaddingTop,
                        end = NexPlaySpacing.screenPaddingX,
                        bottom = NexPlaySpacing.headerPaddingBottom,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NexPlayHeaderActionButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )

            Spacer(
                modifier = Modifier.width(12.dp),
            )

            TextField(
                value = query,
                onValueChange = { nextQuery ->
                    query = nextQuery
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                placeholder = {
                    Text("Search local media")
                },
                trailingIcon =
                    if (query.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = {
                                    query = ""
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    } else {
                        null
                    },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    ),
            )
        }

        if (query.trim().isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                NexPlaySpacing.screenPaddingX,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                NexPlayEmptyState(
                    icon = Icons.Outlined.PermMedia,
                    title = "Search your local media",
                    subtitle =
                        "Type a title, filename, artist, album, or folder name.",
                )
            }
            return@Column
        }

        if (results.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                NexPlaySpacing.screenPaddingX,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                NexPlayEmptyState(
                    icon = Icons.Outlined.PermMedia,
                    title = "No media found",
                    subtitle =
                        "Try another title, filename, artist, or album.",
                )
            }
            return@Column
        }

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
                        top = 4.dp,
                        bottom =
                            NexPlaySpacing.contentPaddingBottom,
                    ),
            ) {
                items(
                    items = results,
                    key = MediaItem::uri,
                ) { item ->
                    SearchResultRow(
                        item = item,
                        onClick = {
                            focusManager.clearFocus()
                            onPlayResult(item)
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

@Composable
private fun SearchResultRow(
    item: MediaItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
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
                text = searchResultSubtitle(item),
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

        Spacer(
            modifier = Modifier.width(12.dp),
        )

        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text =
                    formatSearchResultDuration(
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
    }
}

internal fun filterSearchResults(
    items: List<MediaItem>,
    query: String,
): List<MediaItem> {
    val normalizedQuery =
        query
            .trim()
            .lowercase()

    if (normalizedQuery.isEmpty()) {
        return emptyList()
    }

    return items.filter { item ->
        val searchableValues =
            when (item) {
                is AudioItem ->
                    listOf(
                        item.title,
                        item.displayName,
                        item.artist,
                        item.album,
                        item.folder?.name,
                    )

                is VideoItem ->
                    listOf(
                        item.title,
                        item.displayName,
                        item.folder?.name,
                    )
            }

        searchableValues.any { value ->
            value
                ?.lowercase()
                ?.contains(normalizedQuery)
                ?: false
        }
    }
}

private fun searchResultSubtitle(
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

private fun formatSearchResultDuration(
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
