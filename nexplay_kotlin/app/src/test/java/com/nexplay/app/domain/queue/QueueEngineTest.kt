package com.nexplay.app.domain.queue

import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEngineTest {
    @Test
    fun initialState_isEmpty() {
        val engine = QueueEngine()

        val state = engine.state.value

        assertTrue(state.items.isEmpty())
        assertEquals(-1, state.currentIndex)
        assertNull(state.currentItem)
        assertFalse(state.shuffleEnabled)
        assertEquals(
            RepeatMode.OFF,
            state.repeatMode,
        )
        assertNull(state.playbackContext)
    }

    @Test
    fun setQueue_clampsIndexAndPreservesMixedQueue() {
        val folder =
            FolderInfo(
                name = "Mixed",
                path = "Mixed/",
            )

        val audio =
            audioItem(
                id = "1",
                title = "Audio",
                folder = folder,
            )

        val video =
            videoItem(
                id = "2",
                title = "Video",
                folder = folder,
            )

        val context =
            FolderContext(folder)

        val engine = QueueEngine()

        engine.setQueue(
            items =
                listOf(
                    audio,
                    video,
                ),
            currentIndex = 99,
            playbackContext = context,
        )

        val state = engine.state.value

        assertEquals(
            listOf(audio, video),
            state.items,
        )
        assertEquals(1, state.currentIndex)
        assertEquals(video, state.currentItem)
        assertEquals(
            context,
            state.playbackContext,
        )
    }

    @Test
    fun setQueue_replacesPreviousSourceQueue() {
        val projectFolder =
            FolderInfo(
                name = "Project",
                path = "Project/",
            )

        val stuffFolder =
            FolderInfo(
                name = "Stuff",
                path = "Stuff/",
            )

        val projectItems =
            listOf(
                audioItem(
                    id = "project-1",
                    title = "Project Track",
                    folder = projectFolder,
                ),
                audioItem(
                    id = "ignition",
                    title = "Ignition",
                    folder = projectFolder,
                ),
            )

        val stuffItems =
            listOf(
                audioItem(
                    id = "momentum",
                    title = "Momentum",
                    folder = stuffFolder,
                ),
                audioItem(
                    id = "stuff-2",
                    title = "Stuff Track",
                    folder = stuffFolder,
                ),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = projectItems,
            currentIndex = 1,
            playbackContext =
                FolderContext(projectFolder),
        )

        engine.setQueue(
            items = stuffItems,
            currentIndex = 0,
            playbackContext =
                FolderContext(stuffFolder),
        )

        val state = engine.state.value

        assertEquals(
            stuffItems,
            state.items,
        )
        assertEquals(
            stuffItems[0],
            state.currentItem,
        )
        assertEquals(
            0,
            state.currentIndex,
        )
        assertEquals(
            FolderContext(stuffFolder),
            state.playbackContext,
        )
        assertFalse(
            state.items.any { item ->
                item.folder == projectFolder
            },
        )
    }

    @Test
    fun playAtAndNavigation_followQueueOrder() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
                audioItem("3", "Three"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 0,
            playbackContext =
                SingleItemContext,
        )

        engine.playAt(1)

        assertEquals(
            items[1],
            engine.state.value.currentItem,
        )

        engine.next()

        assertEquals(
            items[2],
            engine.state.value.currentItem,
        )

        engine.previous()

        assertEquals(
            items[1],
            engine.state.value.currentItem,
        )

        engine.playAt(99)

        assertEquals(
            items[1],
            engine.state.value.currentItem,
        )
    }

    @Test
    fun repeatAll_wrapsManualNavigation() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 1,
            playbackContext =
                SingleItemContext,
        )

        engine.setRepeat(
            RepeatMode.ALL,
        )

        engine.next()

        assertEquals(
            0,
            engine.state.value.currentIndex,
        )

        engine.previous()

        assertEquals(
            1,
            engine.state.value.currentIndex,
        )
    }

    @Test
    fun repeatOne_doesNotOverrideManualNavigation() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 0,
            playbackContext =
                SingleItemContext,
        )

        engine.setRepeat(
            RepeatMode.ONE,
        )

        engine.next()

        assertEquals(
            1,
            engine.state.value.currentIndex,
        )

        engine.next()

        assertEquals(
            1,
            engine.state.value.currentIndex,
        )
    }

    @Test
    fun completion_repeatOffAdvancesThenStopsAtBoundary() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 0,
            playbackContext =
                SingleItemContext,
        )

        assertEquals(
            QueueCompletionAction.ADVANCED,
            engine.advanceAfterCompletion(),
        )
        assertEquals(
            1,
            engine.state.value.currentIndex,
        )

        assertEquals(
            QueueCompletionAction.STOP,
            engine.advanceAfterCompletion(),
        )
        assertEquals(
            1,
            engine.state.value.currentIndex,
        )
    }

    @Test
    fun completion_repeatOneRequestsReplayWithoutMovingQueue() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 1,
            playbackContext =
                SingleItemContext,
        )

        engine.setRepeat(
            RepeatMode.ONE,
        )

        assertEquals(
            QueueCompletionAction.REPLAY_CURRENT,
            engine.advanceAfterCompletion(),
        )
        assertEquals(
            1,
            engine.state.value.currentIndex,
        )
        assertEquals(
            items[1],
            engine.state.value.currentItem,
        )
    }

    @Test
    fun completion_repeatAllWrapsAtBoundary() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 1,
            playbackContext =
                SingleItemContext,
        )

        engine.setRepeat(
            RepeatMode.ALL,
        )

        assertEquals(
            QueueCompletionAction.ADVANCED,
            engine.advanceAfterCompletion(),
        )
        assertEquals(
            0,
            engine.state.value.currentIndex,
        )
        assertEquals(
            items[0],
            engine.state.value.currentItem,
        )
    }

    @Test
    fun completion_repeatAllSingleItemRequestsReplay() {
        val item =
            audioItem(
                "1",
                "One",
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = listOf(item),
            currentIndex = 0,
            playbackContext =
                SingleItemContext,
        )

        engine.setRepeat(
            RepeatMode.ALL,
        )

        assertEquals(
            QueueCompletionAction.REPLAY_CURRENT,
            engine.advanceAfterCompletion(),
        )
        assertEquals(
            0,
            engine.state.value.currentIndex,
        )
        assertEquals(
            item,
            engine.state.value.currentItem,
        )
    }

    @Test
    fun toggleShuffle_keepsCurrentAndRestoresOriginalOrder() {

        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
                audioItem("3", "Three"),
                audioItem("4", "Four"),
            )

        val engine =
            QueueEngine(
                random = Random(7),
            )

        engine.setQueue(
            items = items,
            currentIndex = 2,
            playbackContext =
                SingleItemContext,
        )

        val currentItem =
            items[2]

        engine.toggleShuffle()

        val shuffled =
            engine.state.value

        assertTrue(shuffled.shuffleEnabled)
        assertEquals(0, shuffled.currentIndex)
        assertEquals(
            currentItem,
            shuffled.currentItem,
        )
        assertEquals(
            items.map(MediaItem::uri).toSet(),
            shuffled.items
                .map(MediaItem::uri)
                .toSet(),
        )

        engine.toggleShuffle()

        val restored =
            engine.state.value

        assertFalse(restored.shuffleEnabled)
        assertEquals(items, restored.items)
        assertEquals(2, restored.currentIndex)
        assertEquals(
            currentItem,
            restored.currentItem,
        )
    }

    @Test
    fun setQueueWhileShuffleEnabled_keepsSelectedItemCurrent() {
        val engine =
            QueueEngine(
                random = Random(11),
            )

        engine.toggleShuffle()

        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
                audioItem("3", "Three"),
            )

        engine.setQueue(
            items = items,
            currentIndex = 1,
            playbackContext =
                SingleItemContext,
        )

        assertTrue(
            engine.state.value.shuffleEnabled,
        )
        assertEquals(
            items[1],
            engine.state.value.currentItem,
        )
        assertEquals(
            0,
            engine.state.value.currentIndex,
        )

        engine.toggleShuffle()

        assertEquals(
            items,
            engine.state.value.items,
        )
        assertEquals(
            1,
            engine.state.value.currentIndex,
        )
    }

    @Test
    fun appendAndInsertNext_keepCurrentItem() {
        val one =
            audioItem("1", "One")
        val two =
            audioItem("2", "Two")
        val inserted =
            audioItem("3", "Inserted")

        val engine = QueueEngine()

        engine.append(one)

        assertEquals(
            SingleItemContext,
            engine.state.value.playbackContext,
        )
        assertEquals(one, engine.state.value.currentItem)

        engine.append(two)
        engine.insertNext(inserted)

        assertEquals(
            listOf(
                one,
                inserted,
                two,
            ),
            engine.state.value.items,
        )
        assertEquals(
            one,
            engine.state.value.currentItem,
        )
    }

    @Test
    fun removeBeforeCurrent_preservesCurrentItem() {
        val items =
            listOf(
                audioItem("1", "One"),
                audioItem("2", "Two"),
                audioItem("3", "Three"),
            )

        val engine = QueueEngine()

        engine.setQueue(
            items = items,
            currentIndex = 2,
            playbackContext =
                SingleItemContext,
        )

        engine.remove(0)

        assertEquals(
            listOf(
                items[1],
                items[2],
            ),
            engine.state.value.items,
        )
        assertEquals(
            1,
            engine.state.value.currentIndex,
        )
        assertEquals(
            items[2],
            engine.state.value.currentItem,
        )
    }

    @Test
    fun removeCurrent_selectsNextOrPreviousSafely() {
        val one =
            audioItem("1", "One")
        val two =
            audioItem("2", "Two")
        val three =
            audioItem("3", "Three")

        val engine = QueueEngine()

        engine.setQueue(
            items =
                listOf(
                    one,
                    two,
                    three,
                ),
            currentIndex = 1,
            playbackContext =
                SingleItemContext,
        )

        engine.remove(1)

        assertEquals(
            three,
            engine.state.value.currentItem,
        )

        engine.remove(1)

        assertEquals(
            one,
            engine.state.value.currentItem,
        )
        assertEquals(
            0,
            engine.state.value.currentIndex,
        )

        engine.remove(0)

        assertTrue(
            engine.state.value.items.isEmpty(),
        )
        assertNull(
            engine.state.value.currentItem,
        )
        assertNull(
            engine.state.value.playbackContext,
        )
    }

    @Test
    fun move_preservesCurrentItem() {
        val one =
            audioItem("1", "One")
        val two =
            audioItem("2", "Two")
        val three =
            audioItem("3", "Three")

        val engine = QueueEngine()

        engine.setQueue(
            items =
                listOf(
                    one,
                    two,
                    three,
                ),
            currentIndex = 1,
            playbackContext =
                SingleItemContext,
        )

        engine.move(
            fromIndex = 0,
            toIndex = 2,
        )

        assertEquals(
            listOf(
                two,
                three,
                one,
            ),
            engine.state.value.items,
        )
        assertEquals(
            two,
            engine.state.value.currentItem,
        )
        assertEquals(
            0,
            engine.state.value.currentIndex,
        )
    }

    @Test
    fun clear_preservesModesAndClearsQueueContext() {
        val engine = QueueEngine()

        engine.toggleShuffle()
        engine.setRepeat(
            RepeatMode.ALL,
        )

        engine.setQueue(
            items =
                listOf(
                    audioItem("1", "One"),
                ),
            currentIndex = 0,
            playbackContext =
                PlayHubContext(
                    section = "Recently Played",
                ),
        )

        engine.clear()

        val state = engine.state.value

        assertTrue(state.items.isEmpty())
        assertEquals(-1, state.currentIndex)
        assertNull(state.currentItem)
        assertNull(state.playbackContext)
        assertTrue(state.shuffleEnabled)
        assertEquals(
            RepeatMode.ALL,
            state.repeatMode,
        )
    }

    @Test
    fun playbackContexts_keepOriginMetadata() {
        val folder =
            FolderInfo(
                name = "Music",
                path = "Music/",
            )

        assertEquals(
            folder,
            FolderContext(folder).folder,
        )

        assertEquals(
            "playlist-1",
            PlaylistContext(
                playlistId = "playlist-1",
                playlistName = "Workout",
            ).playlistId,
        )

        assertEquals(
            folder,
            SearchContext(
                query = "song",
                sourceFolder = folder,
            ).sourceFolder,
        )

        assertEquals(
            "Recently Played",
            PlayHubContext(
                section = "Recently Played",
            ).section,
        )
    }

    private fun audioItem(
        id: String,
        title: String,
        folder: FolderInfo? = null,
    ): AudioItem {
        return AudioItem(
            id = id,
            uri = "content://audio/$id",
            displayName = "$title.mp3",
            title = title,
            durationMs = 1_000L,
            mimeType = "audio/mpeg",
            folder = folder,
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            artist = null,
            album = null,
            albumId = null,
            track = null,
        )
    }

    private fun videoItem(
        id: String,
        title: String,
        folder: FolderInfo? = null,
    ): VideoItem {
        return VideoItem(
            id = id,
            uri = "content://video/$id",
            displayName = "$title.mp4",
            title = title,
            durationMs = 1_000L,
            mimeType = "video/mp4",
            folder = folder,
            sizeBytes = 1_000L,
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            width = 1920,
            height = 1080,
        )
    }
}