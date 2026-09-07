package com.nexplay.app.data.media

import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryRepositoryTest {
    @Test
    fun refresh_combinesAudioAndVideoIntoCanonicalLibrary() {
        val audio =
            audioItem(
                id = "external_primary:1",
                uri = "content://media/external_primary/audio/media/1",
                title = "Beta",
            )

        val video =
            videoItem(
                id = "external_primary:1",
                uri = "content://media/external_primary/video/media/1",
                title = "Alpha",
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    AudioMediaScanResult.Success(
                        listOf(audio),
                    )
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        listOf(video),
                    )
                },
            )

        val state = repository.refresh()

        assertEquals(
            MediaLibrarySourceStatus.READY,
            state.audioStatus,
        )
        assertEquals(
            MediaLibrarySourceStatus.READY,
            state.videoStatus,
        )

        assertEquals(2, repository.getAllMedia().size)
        assertEquals(
            listOf("Alpha", "Beta"),
            repository.getAllMedia().map { it.title },
        )

        assertEquals(1, repository.getAudio().size)
        assertEquals(1, repository.getVideos().size)

        assertTrue(
            repository.getAllMedia().any {
                it.uri == audio.uri
            },
        )

        assertTrue(
            repository.getAllMedia().any {
                it.uri == video.uri
            },
        )
    }

    @Test
    fun refresh_deduplicatesByUri() {
        val first =
            audioItem(
                id = "1",
                uri = "content://media/external/audio/media/1",
                title = "First",
            )

        val duplicate =
            audioItem(
                id = "different-id",
                uri = first.uri,
                title = "Duplicate",
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    AudioMediaScanResult.Success(
                        listOf(first, duplicate),
                    )
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        emptyList(),
                    )
                },
            )

        repository.refresh()

        assertEquals(1, repository.getAllMedia().size)
        assertEquals("First", repository.getAllMedia().single().title)
    }

    @Test
    fun refresh_successRemovesStaleItems() {
        val retained =
            audioItem(
                id = "1",
                uri = "content://media/external/audio/media/1",
                title = "Retained",
            )

        val removed =
            audioItem(
                id = "2",
                uri = "content://media/external/audio/media/2",
                title = "Removed",
            )

        var audioItems =
            listOf(
                retained,
                removed,
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    AudioMediaScanResult.Success(audioItems)
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        emptyList(),
                    )
                },
            )

        repository.refresh()

        audioItems = listOf(retained)

        repository.refresh()

        assertEquals(1, repository.getAudio().size)
        assertNull(repository.getByUri(removed.uri))
        assertEquals(
            retained,
            repository.getByUri(retained.uri),
        )
    }

    @Test
    fun scannerFailure_preservesLastKnownItems() {
        val audio =
            audioItem(
                id = "1",
                uri = "content://media/external/audio/media/1",
                title = "Song",
            )

        var audioResult: AudioMediaScanResult =
            AudioMediaScanResult.Success(
                listOf(audio),
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    audioResult
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        emptyList(),
                    )
                },
            )

        repository.refresh()

        audioResult =
            AudioMediaScanResult.Failure(
                RuntimeException("temporary failure"),
            )

        val state = repository.refresh()

        assertEquals(
            MediaLibrarySourceStatus.FAILED,
            state.audioStatus,
        )
        assertEquals(listOf(audio), repository.getAudio())
    }

    @Test
    fun permissionDenied_clearsInaccessibleMediaType() {
        val audio =
            audioItem(
                id = "1",
                uri = "content://media/external/audio/media/1",
                title = "Song",
            )

        var audioResult: AudioMediaScanResult =
            AudioMediaScanResult.Success(
                listOf(audio),
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    audioResult
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        emptyList(),
                    )
                },
            )

        repository.refresh()

        audioResult =
            AudioMediaScanResult.PermissionDenied

        val state = repository.refresh()

        assertEquals(
            MediaLibrarySourceStatus.PERMISSION_DENIED,
            state.audioStatus,
        )
        assertTrue(repository.getAudio().isEmpty())
    }

    @Test
    fun folders_areGroupedByPathAndSortedByName() {
        val musicFolder =
            FolderInfo(
                name = "Music",
                path = "Music/",
            )

        val cameraFolder =
            FolderInfo(
                name = "Camera",
                path = "DCIM/Camera/",
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    AudioMediaScanResult.Success(
                        listOf(
                            audioItem(
                                id = "1",
                                uri = "content://audio/1",
                                title = "Song A",
                                folder = musicFolder,
                            ),
                            audioItem(
                                id = "2",
                                uri = "content://audio/2",
                                title = "Song B",
                                folder = musicFolder,
                            ),
                        ),
                    )
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        listOf(
                            videoItem(
                                id = "3",
                                uri = "content://video/3",
                                title = "Video",
                                folder = cameraFolder,
                            ),
                        ),
                    )
                },
            )

        repository.refresh()

        assertEquals(
            listOf(cameraFolder, musicFolder),
            repository.getFolders(),
        )

        assertEquals(
            2,
            repository.getMediaInFolder(musicFolder).size,
        )

        assertEquals(
            1,
            repository.getMediaInFolder(cameraFolder).size,
        )
    }

    @Test
    fun recentlyAdded_isSortedNewestFirst() {
        val oldItem =
            audioItem(
                id = "1",
                uri = "content://audio/1",
                title = "Old",
                dateAddedEpochSeconds = 100L,
            )

        val newItem =
            videoItem(
                id = "2",
                uri = "content://video/2",
                title = "New",
                dateAddedEpochSeconds = 300L,
            )

        val middleItem =
            audioItem(
                id = "3",
                uri = "content://audio/3",
                title = "Middle",
                dateAddedEpochSeconds = 200L,
            )

        val repository =
            MediaLibraryRepository(
                scanAudio = {
                    AudioMediaScanResult.Success(
                        listOf(oldItem, middleItem),
                    )
                },
                scanVideo = {
                    VideoMediaScanResult.Success(
                        listOf(newItem),
                    )
                },
            )

        repository.refresh()

        assertEquals(
            listOf("New", "Middle", "Old"),
            repository
                .getRecentlyAdded()
                .map { it.title },
        )
    }

    private fun audioItem(
        id: String,
        uri: String,
        title: String,
        folder: FolderInfo? = null,
        dateAddedEpochSeconds: Long? = null,
    ): AudioItem {
        return AudioItem(
            id = id,
            uri = uri,
            displayName = "$title.mp3",
            title = title,
            durationMs = 1_000L,
            mimeType = "audio/mpeg",
            folder = folder,
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = dateAddedEpochSeconds,
            dateModifiedEpochSeconds = null,
            artist = null,
            album = null,
            albumId = null,
            track = null,
        )
    }

    private fun videoItem(
        id: String,
        uri: String,
        title: String,
        folder: FolderInfo? = null,
        dateAddedEpochSeconds: Long? = null,
    ): VideoItem {
        return VideoItem(
            id = id,
            uri = uri,
            displayName = "$title.mp4",
            title = title,
            durationMs = 1_000L,
            mimeType = "video/mp4",
            folder = folder,
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = dateAddedEpochSeconds,
            dateModifiedEpochSeconds = null,
            width = 1920,
            height = 1080,
        )
    }
}