package com.nexplay.app.data.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaLibraryRepositoryInstrumentedTest {
    @Test
    fun refresh_aggregatesRealAudioAndVideoMedia() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val repository =
            MediaLibraryRepository(context)

        val state = repository.refresh()

        assertEquals(
            MediaLibrarySourceStatus.READY,
            state.audioStatus,
        )

        assertEquals(
            MediaLibrarySourceStatus.READY,
            state.videoStatus,
        )

        assertFalse(repository.getAudio().isEmpty())
        assertFalse(repository.getVideos().isEmpty())

        assertEquals(
            repository.getAudio().size +
                    repository.getVideos().size,
            repository.getAllMedia().size,
        )

        repository.getAllMedia().forEach { item ->
            assertTrue(item.id.isNotBlank())
            assertTrue(item.uri.startsWith("content://"))
            assertTrue(item.title.isNotBlank())
        }
    }
}