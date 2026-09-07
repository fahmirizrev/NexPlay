package com.nexplay.app.data.media

import com.nexplay.app.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoMediaMapperTest {
    @Test
    fun validRow_mapsToCanonicalVideoItem() {
        val item =
            VideoMediaMapper.map(
                VideoMediaRow(
                    id = "external_primary:42",
                    uri = "content://media/external_primary/video/media/42",
                    displayName = " video.mp4 ",
                    title = "Video",
                    durationMs = 60_000L,
                    mimeType = "video/mp4",
                    folderName = "Movies",
                    folderPath = "Movies/",
                    sizeBytes = 20_000_000L,
                    dateAddedEpochSeconds = 1_700_000_000L,
                    dateModifiedEpochSeconds = 1_700_000_100L,
                    width = 1920,
                    height = 1080,
                ),
            )

        requireNotNull(item)

        assertEquals("external_primary:42", item.id)
        assertEquals("video.mp4", item.displayName)
        assertEquals("Video", item.title)
        assertEquals(MediaType.VIDEO, item.mediaType)
        assertEquals(60_000L, item.durationMs)
        assertEquals("Movies", item.folder?.name)
        assertEquals(1920, item.width)
        assertEquals(1080, item.height)
        assertEquals("1920x1080", item.resolution)
    }

    @Test
    fun missingTitleAndInvalidDimensions_areNormalized() {
        val item =
            VideoMediaMapper.map(
                VideoMediaRow(
                    id = "1",
                    uri = "content://media/external/video/media/1",
                    displayName = "clip.mp4",
                    title = "<unknown>",
                    durationMs = 0L,
                    mimeType = "video/mp4",
                    folderName = null,
                    folderPath = "DCIM/Camera/",
                    sizeBytes = 0L,
                    dateAddedEpochSeconds = 0L,
                    dateModifiedEpochSeconds = 0L,
                    width = 0,
                    height = -1,
                ),
            )

        requireNotNull(item)

        assertEquals("clip", item.title)
        assertEquals("Camera", item.folder?.name)
        assertNull(item.durationMs)
        assertNull(item.sizeBytes)
        assertNull(item.width)
        assertNull(item.height)
        assertNull(item.resolution)
    }

    @Test
    fun missingIdentity_isRejected() {
        val item =
            VideoMediaMapper.map(
                VideoMediaRow(
                    id = null,
                    uri = null,
                    displayName = "video.mp4",
                    title = "Video",
                    durationMs = null,
                    mimeType = null,
                    folderName = null,
                    folderPath = null,
                    sizeBytes = null,
                    dateAddedEpochSeconds = null,
                    dateModifiedEpochSeconds = null,
                    width = null,
                    height = null,
                ),
            )

        assertNull(item)
    }
}