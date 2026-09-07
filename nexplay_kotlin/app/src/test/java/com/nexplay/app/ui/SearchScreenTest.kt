package com.nexplay.app.ui

import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenTest {
    @Test
    fun filterSearchResults_emptyQueryReturnsNoResults() {
        assertTrue(
            filterSearchResults(
                items = listOf(audioItem()),
                query = "   ",
            ).isEmpty(),
        )
    }

    @Test
    fun filterSearchResults_matchesSupportedFieldsCaseInsensitively() {
        val audio = audioItem()
        val items: List<MediaItem> =
            listOf(
                audio,
                videoItem(),
            )

        assertEquals(
            listOf(audio),
            filterSearchResults(
                items = items,
                query = "canonical title",
            ),
        )
        assertEquals(
            listOf(audio),
            filterSearchResults(
                items = items,
                query = "FILENAME SONG",
            ),
        )
        assertEquals(
            listOf(audio),
            filterSearchResults(
                items = items,
                query = "ARTIST NAME",
            ),
        )
        assertEquals(
            listOf(audio),
            filterSearchResults(
                items = items,
                query = "album name",
            ),
        )
        assertEquals(
            listOf(audio),
            filterSearchResults(
                items = items,
                query = "music folder",
            ),
        )
    }

    @Test
    fun filterSearchResults_returnsMatchingVideoFile() {
        val video = videoItem()

        assertEquals(
            listOf(video),
            filterSearchResults(
                items =
                    listOf(
                        audioItem(),
                        video,
                    ),
                query = "holiday clip",
            ),
        )
    }

    private fun audioItem(): AudioItem {
        return AudioItem(
            id = "audio-1",
            uri = "content://audio/1",
            displayName = "Filename Song.mp3",
            title = "Canonical Title",
            durationMs = 61_000L,
            mimeType = "audio/mpeg",
            folder =
                FolderInfo(
                    name = "Music Folder",
                    path = "/Music Folder",
                ),
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = 1L,
            dateModifiedEpochSeconds = 1L,
            artist = "Artist Name",
            album = "Album Name",
            albumId = "album-1",
            track = 1,
        )
    }

    private fun videoItem(): VideoItem {
        return VideoItem(
            id = "video-1",
            uri = "content://video/1",
            displayName = "Holiday Clip.mp4",
            title = "Holiday Clip",
            durationMs = 120_000L,
            mimeType = "video/mp4",
            folder =
                FolderInfo(
                    name = "Camera",
                    path = "/Camera",
                ),
            sizeBytes = 2_000L,
            dateAddedEpochSeconds = 1L,
            dateModifiedEpochSeconds = 1L,
            width = 1920,
            height = 1080,
        )
    }
}
