package com.nexplay.app.data.media

import com.nexplay.app.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioMediaMapperTest {
    @Test
    fun validRow_mapsToCanonicalAudioItem() {
        val item =
            AudioMediaMapper.map(
                AudioMediaRow(
                    id = "external_primary:42",
                    uri = "content://media/external_primary/audio/media/42",
                    displayName = " song.mp3 ",
                    title = "Song",
                    durationMs = 180_000L,
                    mimeType = "audio/mpeg",
                    folderName = "Music",
                    folderPath = "Music/",
                    sizeBytes = 4_000_000L,
                    dateAddedEpochSeconds = 1_700_000_000L,
                    dateModifiedEpochSeconds = 1_700_000_100L,
                    artist = "Artist",
                    album = "Album",
                    albumId = "9",
                    track = 1,
                ),
            )

        requireNotNull(item)

        assertEquals("external_primary:42", item.id)
        assertEquals("song.mp3", item.displayName)
        assertEquals("Song", item.title)
        assertEquals(MediaType.AUDIO, item.mediaType)
        assertEquals(180_000L, item.durationMs)
        assertEquals("Music", item.folder?.name)
        assertEquals("Music/", item.folder?.path)
        assertEquals("Artist", item.artist)
        assertEquals("Album", item.album)
        assertEquals("9", item.albumId)
        assertEquals(1, item.track)
    }

    @Test
    fun unknownMetadata_isNormalizedAndTitleFallsBackToFilename() {
        val item =
            AudioMediaMapper.map(
                AudioMediaRow(
                    id = "1",
                    uri = "content://media/external/audio/media/1",
                    displayName = "track.mp3",
                    title = "<unknown>",
                    durationMs = 0L,
                    mimeType = "audio/mpeg",
                    folderName = null,
                    folderPath = "Music/Album/",
                    sizeBytes = 0L,
                    dateAddedEpochSeconds = 0L,
                    dateModifiedEpochSeconds = 0L,
                    artist = "<unknown>",
                    album = "<unknown>",
                    albumId = null,
                    track = 0,
                ),
            )

        requireNotNull(item)

        assertEquals("track", item.title)
        assertEquals("Album", item.folder?.name)
        assertNull(item.durationMs)
        assertNull(item.sizeBytes)
        assertNull(item.artist)
        assertNull(item.album)
        assertNull(item.track)
    }

    @Test
    fun missingIdentity_isRejected() {
        val item =
            AudioMediaMapper.map(
                AudioMediaRow(
                    id = null,
                    uri = null,
                    displayName = "track.mp3",
                    title = "Track",
                    durationMs = null,
                    mimeType = null,
                    folderName = null,
                    folderPath = null,
                    sizeBytes = null,
                    dateAddedEpochSeconds = null,
                    dateModifiedEpochSeconds = null,
                    artist = null,
                    album = null,
                    albumId = null,
                    track = null,
                ),
            )

        assertNull(item)
    }
}