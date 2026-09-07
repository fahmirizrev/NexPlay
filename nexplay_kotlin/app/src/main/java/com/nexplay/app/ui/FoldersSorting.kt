package com.nexplay.app.ui

import com.nexplay.app.data.preferences.NexPlayPreferences
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem

internal enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

internal enum class FolderSortField {
    NAME,
    MEDIA_COUNT,
    DATE_MODIFIED,
}

internal enum class MediaSortField {
    TITLE,
    ALBUM,
    TRACK,
    DURATION,
    DATE_MODIFIED,
}

internal data class SortConfig<T>(
    val field: T,
    val direction: SortDirection,
) {
    fun toggle(
        nextField: T,
    ): SortConfig<T> {
        return if (
            field == nextField &&
            direction == SortDirection.ASCENDING
        ) {
            SortConfig(
                field = nextField,
                direction = SortDirection.DESCENDING,
            )
        } else {
            SortConfig(
                field = nextField,
                direction = SortDirection.ASCENDING,
            )
        }
    }
}

internal val defaultFolderSort =
    SortConfig(
        field = FolderSortField.NAME,
        direction = SortDirection.ASCENDING,
    )

internal val defaultMediaSort =
    SortConfig(
        field = MediaSortField.TITLE,
        direction = SortDirection.ASCENDING,
    )

internal fun NexPlayPreferences.resolveFolderSortConfig():
        SortConfig<FolderSortField> {
    val field =
        FolderSortField.entries
            .firstOrNull { candidate ->
                candidate.name ==
                    folderSortFieldName
            }
            ?: defaultFolderSort.field

    val direction =
        SortDirection.entries
            .firstOrNull { candidate ->
                candidate.name ==
                    folderSortDirectionName
            }
            ?: defaultFolderSort.direction

    return SortConfig(
        field = field,
        direction = direction,
    )
}

internal fun NexPlayPreferences.resolveMediaSortConfig():
        SortConfig<MediaSortField> {
    val field =
        MediaSortField.entries
            .firstOrNull { candidate ->
                candidate.name ==
                    mediaSortFieldName
            }
            ?: defaultMediaSort.field

    val direction =
        SortDirection.entries
            .firstOrNull { candidate ->
                candidate.name ==
                    mediaSortDirectionName
            }
            ?: defaultMediaSort.direction

    return SortConfig(
        field = field,
        direction = direction,
    )
}

internal data class FolderListItem(
    val folder: FolderInfo,
    val audioCount: Int,
    val videoCount: Int,
    val dateModifiedEpochSeconds: Long?,
) {
    val mediaCount: Int
        get() = audioCount + videoCount
}

internal fun buildFolderListItems(
    folders: List<FolderInfo>,
    mediaItems: List<MediaItem>,
): List<FolderListItem> {
    val itemsByFolder =
        mediaItems
            .mapNotNull { item ->
                item.folder?.let { folder ->
                    folderKey(folder) to item
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )

    return folders.map { folder ->
        val folderItems =
            itemsByFolder[folderKey(folder)]
                .orEmpty()

        FolderListItem(
            folder = folder,
            audioCount =
                folderItems.count { item ->
                    item is AudioItem
                },
            videoCount =
                folderItems.count { item ->
                    item is VideoItem
                },
            dateModifiedEpochSeconds =
                folderItems
                    .mapNotNull { item ->
                        item.dateModifiedEpochSeconds
                    }
                    .maxOrNull(),
        )
    }
}

internal fun sortFolderItems(
    items: List<FolderListItem>,
    config: SortConfig<FolderSortField>,
): List<FolderListItem> {
    return items.sortedWith { first, second ->
        val comparison =
            when (config.field) {
                FolderSortField.NAME ->
                    compareText(
                        first.folder.name,
                        second.folder.name,
                    )

                FolderSortField.MEDIA_COUNT ->
                    first.mediaCount.compareTo(
                        second.mediaCount,
                    )

                FolderSortField.DATE_MODIFIED ->
                    (first.dateModifiedEpochSeconds ?: 0L)
                        .compareTo(
                            second.dateModifiedEpochSeconds
                                ?: 0L,
                        )
            }

        applyDirection(
            comparison,
            config.direction,
        )
    }
}

internal fun sortMediaItems(
    items: List<MediaItem>,
    config: SortConfig<MediaSortField>,
): List<MediaItem> {
    return items.sortedWith { first, second ->
        val comparison =
            when (config.field) {
                MediaSortField.TITLE ->
                    compareText(
                        first.title,
                        second.title,
                    )

                MediaSortField.ALBUM ->
                    compareText(
                        (first as? AudioItem)
                            ?.album
                            .orEmpty(),
                        (second as? AudioItem)
                            ?.album
                            .orEmpty(),
                    )

                MediaSortField.TRACK ->
                    (
                        (first as? AudioItem)
                            ?.track
                            ?: Int.MAX_VALUE
                    ).compareTo(
                        (second as? AudioItem)
                            ?.track
                            ?: Int.MAX_VALUE,
                    )

                MediaSortField.DURATION ->
                    (first.durationMs ?: 0L)
                        .compareTo(
                            second.durationMs ?: 0L,
                        )

                MediaSortField.DATE_MODIFIED ->
                    (first.dateModifiedEpochSeconds ?: 0L)
                        .compareTo(
                            second.dateModifiedEpochSeconds
                                ?: 0L,
                        )
            }

        val resolvedComparison =
            if (comparison == 0) {
                compareText(
                    first.title,
                    second.title,
                )
            } else {
                comparison
            }

        applyDirection(
            resolvedComparison,
            config.direction,
        )
    }
}

internal fun folderSortLabel(
    field: FolderSortField,
): String {
    return when (field) {
        FolderSortField.NAME ->
            "Folder name"

        FolderSortField.MEDIA_COUNT ->
            "Media count"

        FolderSortField.DATE_MODIFIED ->
            "Date modified"
    }
}

internal fun mediaSortLabel(
    field: MediaSortField,
): String {
    return when (field) {
        MediaSortField.TITLE ->
            "Title"

        MediaSortField.ALBUM ->
            "Album"

        MediaSortField.TRACK ->
            "Track"

        MediaSortField.DURATION ->
            "Duration"

        MediaSortField.DATE_MODIFIED ->
            "Date Modified"
    }
}

internal fun folderKey(
    folder: FolderInfo,
): String {
    return folder.path?.let { path ->
        "path:$path"
    } ?: "name:${folder.name}"
}

private fun compareText(
    first: String,
    second: String,
): Int {
    return first.compareTo(
        second,
        ignoreCase = true,
    )
}

private fun applyDirection(
    comparison: Int,
    direction: SortDirection,
): Int {
    return if (
        direction == SortDirection.ASCENDING
    ) {
        comparison
    } else {
        -comparison
    }
}