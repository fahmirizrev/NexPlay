package com.nexplay.app.ui

import com.nexplay.app.data.preferences.NexPlayPreferences
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class FoldersSortingTest {
    @Test
    fun sortConfig_toggleMatchesFlutterBehavior() {
        val initial = defaultFolderSort

        val descending =
            initial.toggle(
                FolderSortField.NAME,
            )

        assertEquals(
            SortDirection.DESCENDING,
            descending.direction,
        )

        val ascendingAgain =
            descending.toggle(
                FolderSortField.NAME,
            )

        assertEquals(
            SortDirection.ASCENDING,
            ascendingAgain.direction,
        )

        val anotherField =
            descending.toggle(
                FolderSortField.MEDIA_COUNT,
            )

        assertEquals(
            FolderSortField.MEDIA_COUNT,
            anotherField.field,
        )
        assertEquals(
            SortDirection.ASCENDING,
            anotherField.direction,
        )
    }

    @Test
    fun folderItems_sortByMediaCount() {
        val first =
            FolderListItem(
                folder =
                    FolderInfo(
                        name = "One",
                        path = "One/",
                    ),
                audioCount = 1,
                videoCount = 0,
                dateModifiedEpochSeconds = 100L,
            )

        val second =
            FolderListItem(
                folder =
                    FolderInfo(
                        name = "Two",
                        path = "Two/",
                    ),
                audioCount = 2,
                videoCount = 3,
                dateModifiedEpochSeconds = 200L,
            )

        val result =
            sortFolderItems(
                listOf(second, first),
                SortConfig(
                    field =
                        FolderSortField.MEDIA_COUNT,
                    direction =
                        SortDirection.ASCENDING,
                ),
            )

        assertEquals(
            listOf("One", "Two"),
            result.map { item ->
                item.folder.name
            },
        )
    }

    @Test
    fun folderItems_dateModifiedUsesNewestMediaInFolder() {
        val folder =
            FolderInfo(
                name = "Music",
                path = "Music/",
            )

        val folders =
            buildFolderListItems(
                folders = listOf(folder),
                mediaItems =
                    listOf(
                        audioItem(
                            id = "1",
                            title = "Old",
                            folder = folder,
                            dateModified = 100L,
                        ),
                        audioItem(
                            id = "2",
                            title = "New",
                            folder = folder,
                            dateModified = 500L,
                        ),
                    ),
            )

        assertEquals(
            500L,
            folders.single()
                .dateModifiedEpochSeconds,
        )
    }

    @Test
    fun defaultMediaSort_isTitleAscending() {
        assertEquals(
            MediaSortField.TITLE,
            defaultMediaSort.field,
        )
        assertEquals(
            SortDirection.ASCENDING,
            defaultMediaSort.direction,
        )
    }

    @Test
    fun legacyNamePreference_fallsBackToTitle() {
        val config =
            NexPlayPreferences(
                mediaSortFieldName =
                    "NAME",
            ).resolveMediaSortConfig()

        assertEquals(
            MediaSortField.TITLE,
            config.field,
        )
    }

    @Test
    fun mediaItems_sortByAlbumAscending() {
        val beta =
            audioItem(
                id = "1",
                title = "Beta Song",
                album = "Beta",
            )

        val alpha =
            audioItem(
                id = "2",
                title = "Alpha Song",
                album = "Alpha",
            )

        val result =
            sortMediaItems(
                listOf(beta, alpha),
                SortConfig(
                    field =
                        MediaSortField.ALBUM,
                    direction =
                        SortDirection.ASCENDING,
                ),
            )

        assertEquals(
            listOf(
                "Alpha Song",
                "Beta Song",
            ),
            result.map(
                MediaItem::title,
            ),
        )
    }

    @Test
    fun mediaItems_sortByTrackAscending() {
        val second =
            audioItem(
                id = "1",
                title = "Second",
                track = 2,
            )

        val first =
            audioItem(
                id = "2",
                title = "First",
                track = 1,
            )

        val video =
            videoItem(
                id = "3",
                title = "Video",
            )

        val result =
            sortMediaItems(
                listOf(
                    second,
                    video,
                    first,
                ),
                SortConfig(
                    field =
                        MediaSortField.TRACK,
                    direction =
                        SortDirection.ASCENDING,
                ),
            )

        assertEquals(
            listOf(
                "First",
                "Second",
                "Video",
            ),
            result.map(
                MediaItem::title,
            ),
        )
    }

    @Test
    fun mediaItems_sortByDurationDescending() {
        val short =
            audioItem(
                id = "1",
                title = "Short",
                duration = 1_000L,
            )

        val long =
            videoItem(
                id = "2",
                title = "Long",
                duration = 10_000L,
            )

        val result =
            sortMediaItems(
                listOf(short, long),
                SortConfig(
                    field = MediaSortField.DURATION,
                    direction =
                        SortDirection.DESCENDING,
                ),
            )

        assertEquals(
            listOf("Long", "Short"),
            result.map(MediaItem::title),
        )
    }

    private fun audioItem(
        id: String,
        title: String,
        folder: FolderInfo? = null,
        duration: Long? = 1_000L,
        dateModified: Long? = null,
        album: String? = null,
        track: Int? = null,
    ): AudioItem {
        return AudioItem(
            id = id,
            uri = "content://audio/$id",
            displayName = "$title.mp3",
            title = title,
            durationMs = duration,
            mimeType = "audio/mpeg",
            folder = folder,
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds =
                dateModified,
            artist = null,
            album = album,
            albumId = null,
            track = track,
        )
    }

    private fun videoItem(
        id: String,
        title: String,
        duration: Long? = 1_000L,
    ): VideoItem {
        return VideoItem(
            id = id,
            uri = "content://video/$id",
            displayName = "$title.mp4",
            title = title,
            durationMs = duration,
            mimeType = "video/mp4",
            folder = null,
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            width = 1920,
            height = 1080,
        )
    }
}