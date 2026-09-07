package com.nexplay.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemTest {
    @Test
    fun audioItem_hasAudioMediaType() {
        val item = AudioItem(
            id = "1",
            uri = "content://media/audio/1",
            displayName = "song.mp3",
            title = "Song",
            durationMs = 180_000L,
            mimeType = "audio/mpeg",
            folder = FolderInfo(
                name = "Music",
                path = "Music/",
            ),
            sizeBytes = 4_000_000L,
            dateAddedEpochSeconds = 1_700_000_000L,
            dateModifiedEpochSeconds = 1_700_000_100L,
            artist = "Artist",
            album = "Album",
            albumId = "10",
            track = 1,
        )

        assertEquals(MediaType.AUDIO, item.mediaType)
    }

    @Test
    fun videoItem_hasVideoMediaTypeAndDerivedResolution() {
        val item = VideoItem(
            id = "2",
            uri = "content://media/video/2",
            displayName = "video.mp4",
            title = "Video",
            durationMs = 60_000L,
            mimeType = "video/mp4",
            folder = FolderInfo(
                name = "Movies",
                path = "Movies/",
            ),
            sizeBytes = 20_000_000L,
            dateAddedEpochSeconds = 1_700_000_000L,
            dateModifiedEpochSeconds = 1_700_000_100L,
            width = 1920,
            height = 1080,
        )

        assertEquals(MediaType.VIDEO, item.mediaType)
        assertEquals("1920x1080", item.resolution)
    }

    @Test
    fun videoItem_withoutCompleteDimensions_hasNoResolution() {
        val item = VideoItem(
            id = "3",
            uri = "content://media/video/3",
            displayName = "video.mp4",
            title = "Video",
            durationMs = null,
            mimeType = null,
            folder = null,
            sizeBytes = null,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            width = 1920,
            height = null,
        )

        assertNull(item.resolution)
    }

    @Test
    fun audioAndVideo_areCanonicalMediaItems() {
        val audio: MediaItem = AudioItem(
            id = "1",
            uri = "content://media/audio/1",
            displayName = "song.mp3",
            title = "Song",
            durationMs = null,
            mimeType = null,
            folder = null,
            sizeBytes = null,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            artist = null,
            album = null,
            albumId = null,
            track = null,
        )

        val video: MediaItem = VideoItem(
            id = "2",
            uri = "content://media/video/2",
            displayName = "video.mp4",
            title = "Video",
            durationMs = null,
            mimeType = null,
            folder = null,
            sizeBytes = null,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            width = null,
            height = null,
        )

        assertTrue(audio is AudioItem)
        assertTrue(video is VideoItem)
    }
}