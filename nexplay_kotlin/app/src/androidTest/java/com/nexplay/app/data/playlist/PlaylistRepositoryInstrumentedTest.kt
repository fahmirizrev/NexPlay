package com.nexplay.app.data.playlist

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistRepositoryInstrumentedTest {
    @Test
    fun playlistCrudReorderAndPersistence_surviveDatabaseReopen() =
        runBlocking {
            val context =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext

            val databaseName =
                "playlist_test_" +
                        System.nanoTime() +
                        ".db"

            context.deleteDatabase(
                databaseName,
            )

            var database =
                NexPlayDatabase.create(
                    context = context,
                    databaseName =
                        databaseName,
                )

            try {
                var repository =
                    PlaylistRepository(
                        database,
                    )

                val playlistId =
                    repository
                        .createPlaylist(
                            name = "Practice",
                            initialMediaUris =
                                listOf(
                                    "content://media/audio/1",
                                    "content://media/audio/2",
                                ),
                        )

                assertFalse(
                    repository.addMedia(
                        playlistId =
                            playlistId,
                        mediaUri =
                            "content://media/audio/1",
                    ),
                )

                assertTrue(
                    repository.addMedia(
                        playlistId =
                            playlistId,
                        mediaUri =
                            "content://media/video/3",
                    ),
                )

                var detail =
                    repository
                        .observePlaylist(
                            playlistId,
                        )
                        .filterNotNull()
                        .first()

                assertEquals(
                    listOf(
                        "content://media/audio/1",
                        "content://media/audio/2",
                        "content://media/video/3",
                    ),
                    detail.items.map {
                        it.mediaUri
                    },
                )

                assertTrue(
                    repository.reorder(
                        playlistId =
                            playlistId,
                        fromIndex = 0,
                        toIndex = 2,
                    ),
                )

                detail =
                    repository
                        .observePlaylist(
                            playlistId,
                        )
                        .filterNotNull()
                        .first()

                assertEquals(
                    listOf(
                        "content://media/audio/2",
                        "content://media/video/3",
                        "content://media/audio/1",
                    ),
                    detail.items.map {
                        it.mediaUri
                    },
                )

                database.close()

                database =
                    NexPlayDatabase.create(
                        context = context,
                        databaseName =
                            databaseName,
                    )

                repository =
                    PlaylistRepository(
                        database,
                    )

                val persisted =
                    repository
                        .observePlaylist(
                            playlistId,
                        )
                        .filterNotNull()
                        .first()

                assertEquals(
                    "Practice",
                    persisted.name,
                )

                assertEquals(
                    listOf(
                        "content://media/audio/2",
                        "content://media/video/3",
                        "content://media/audio/1",
                    ),
                    persisted.items.map {
                        it.mediaUri
                    },
                )

                assertTrue(
                    repository.renamePlaylist(
                        playlistId =
                            playlistId,
                        name =
                            "Band Practice",
                    ),
                )

                val renamed =
                    repository
                        .observePlaylist(
                            playlistId,
                        )
                        .filterNotNull()
                        .first()

                assertEquals(
                    "Band Practice",
                    renamed.name,
                )

                assertTrue(
                    repository.removeItem(
                        playlistId =
                            playlistId,
                        playlistItemId =
                            renamed.items
                                .first()
                                .id,
                    ),
                )

                assertTrue(
                    repository.clearPlaylist(
                        playlistId,
                    ),
                )

                assertTrue(
                    repository.deletePlaylist(
                        playlistId,
                    ),
                )

                assertTrue(
                    repository
                        .playlists
                        .first()
                        .isEmpty(),
                )
            } finally {
                database.close()

                context.deleteDatabase(
                    databaseName,
                )
            }
        }
}
