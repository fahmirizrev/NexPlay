package com.nexplay.app.data.playlist

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class PlaylistSummary(
    val id: String,
    val name: String,
    val itemCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class PlaylistItem(
    val id: String,
    val mediaUri: String,
    val position: Int,
    val addedAtEpochMillis: Long,
)

data class PlaylistDetail(
    val id: String,
    val name: String,
    val items: List<PlaylistItem>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

class PlaylistRepository(
    private val database:
    NexPlayDatabase,
    private val dao:
    PlaylistDao =
        database.playlistDao(),
) {
    val playlists:
            Flow<List<PlaylistSummary>> =
        dao
            .observePlaylistSummaries()
            .map { rows ->
                rows.map { row ->
                    PlaylistSummary(
                        id = row.id,
                        name = row.name,
                        itemCount =
                            row.itemCount
                                .coerceAtMost(
                                    Int.MAX_VALUE
                                        .toLong(),
                                )
                                .toInt(),
                        createdAtEpochMillis =
                            row.createdAtEpochMillis,
                        updatedAtEpochMillis =
                            row.updatedAtEpochMillis,
                    )
                }
            }

    fun observePlaylist(
        playlistId: String,
    ): Flow<PlaylistDetail?> {
        return combine(
            dao.observePlaylist(
                playlistId,
            ),
            dao.observePlaylistItems(
                playlistId,
            ),
        ) {
                playlist,
                items ->
            playlist?.let { entity ->
                PlaylistDetail(
                    id = entity.id,
                    name = entity.name,
                    items =
                        items.map { item ->
                            PlaylistItem(
                                id = item.id,
                                mediaUri =
                                    item.mediaUri,
                                position =
                                    item.position,
                                addedAtEpochMillis =
                                    item.addedAtEpochMillis,
                            )
                        },
                    createdAtEpochMillis =
                        entity.createdAtEpochMillis,
                    updatedAtEpochMillis =
                        entity.updatedAtEpochMillis,
                )
            }
        }
    }

    suspend fun createPlaylist(
        name: String,
        initialMediaUris:
        List<String> =
            emptyList(),
    ): String {
        val playlistId =
            UUID.randomUUID()
                .toString()
        val now =
            System.currentTimeMillis()

        database.withTransaction {
            val resolvedName =
                buildUniqueName(
                    baseName =
                        sanitizeCreateName(
                            name,
                        ),
                    exceptPlaylistId =
                        null,
                )

            dao.insertPlaylist(
                PlaylistEntity(
                    id = playlistId,
                    name = resolvedName,
                    createdAtEpochMillis =
                        now,
                    updatedAtEpochMillis =
                        now,
                ),
            )

            insertMediaInternal(
                playlistId =
                    playlistId,
                mediaUris =
                    initialMediaUris,
                now = now,
                touchPlaylist = false,
            )
        }

        return playlistId
    }

    suspend fun renamePlaylist(
        playlistId: String,
        name: String,
    ): Boolean {
        val sanitizedName =
            sanitizeRenameName(
                name,
            )
                ?: return false

        return database.withTransaction {
            val playlist =
                dao.getPlaylist(
                    playlistId,
                )
                    ?: return@withTransaction false

            val resolvedName =
                buildUniqueName(
                    baseName =
                        sanitizedName,
                    exceptPlaylistId =
                        playlistId,
                )

            dao.updatePlaylist(
                playlist.copy(
                    name = resolvedName,
                    updatedAtEpochMillis =
                        System.currentTimeMillis(),
                ),
            )

            true
        }
    }

    suspend fun deletePlaylist(
        playlistId: String,
    ): Boolean {
        return database.withTransaction {
            dao.deletePlaylist(
                playlistId,
            ) > 0
        }
    }

    suspend fun clearPlaylist(
        playlistId: String,
    ): Boolean {
        return database.withTransaction {
            val playlist =
                dao.getPlaylist(
                    playlistId,
                )
                    ?: return@withTransaction false

            val removedCount =
                dao.clearPlaylistItems(
                    playlistId,
                )

            if (removedCount <= 0) {
                return@withTransaction false
            }

            dao.updatePlaylist(
                playlist.copy(
                    updatedAtEpochMillis =
                        System.currentTimeMillis(),
                ),
            )

            true
        }
    }

    suspend fun addMedia(
        playlistId: String,
        mediaUri: String,
    ): Boolean {
        return addMedia(
            playlistId =
                playlistId,
            mediaUris =
                listOf(
                    mediaUri,
                ),
        )
    }

    suspend fun addMedia(
        playlistId: String,
        mediaUris: List<String>,
    ): Boolean {
        return database.withTransaction {
            if (
                dao.getPlaylist(
                    playlistId,
                ) == null
            ) {
                return@withTransaction false
            }

            insertMediaInternal(
                playlistId =
                    playlistId,
                mediaUris =
                    mediaUris,
                now =
                    System.currentTimeMillis(),
                touchPlaylist = true,
            )
        }
    }

    suspend fun removeItem(
        playlistId: String,
        playlistItemId: String,
    ): Boolean {
        return database.withTransaction {
            val playlist =
                dao.getPlaylist(
                    playlistId,
                )
                    ?: return@withTransaction false

            if (
                dao.deletePlaylistItem(
                    playlistItemId,
                ) <= 0
            ) {
                return@withTransaction false
            }

            normalizePositions(
                playlistId,
            )

            dao.updatePlaylist(
                playlist.copy(
                    updatedAtEpochMillis =
                        System.currentTimeMillis(),
                ),
            )

            true
        }
    }

    suspend fun reorder(
        playlistId: String,
        fromIndex: Int,
        toIndex: Int,
    ): Boolean {
        return database.withTransaction {
            val playlist =
                dao.getPlaylist(
                    playlistId,
                )
                    ?: return@withTransaction false

            val items =
                dao.getPlaylistItems(
                    playlistId,
                )
                    .toMutableList()

            if (
                fromIndex !in
                items.indices ||
                toIndex !in
                items.indices ||
                fromIndex == toIndex
            ) {
                return@withTransaction false
            }

            val item =
                items.removeAt(
                    fromIndex,
                )

            items.add(
                toIndex,
                item,
            )

            dao.updatePlaylistItems(
                items.mapIndexed {
                        index,
                        entry ->
                    entry.copy(
                        position = index,
                    )
                },
            )

            dao.updatePlaylist(
                playlist.copy(
                    updatedAtEpochMillis =
                        System.currentTimeMillis(),
                ),
            )

            true
        }
    }

    private suspend fun insertMediaInternal(
        playlistId: String,
        mediaUris: List<String>,
        now: Long,
        touchPlaylist: Boolean,
    ): Boolean {
        val normalizedUris =
            mediaUris
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )
                .distinct()

        if (normalizedUris.isEmpty()) {
            return false
        }

        val existingUris =
            dao.getMediaUris(
                playlistId,
            )
                .toHashSet()

        val newUris =
            normalizedUris.filterNot(
                existingUris::contains,
            )

        if (newUris.isEmpty()) {
            return false
        }

        val startPosition =
            dao.getNextPosition(
                playlistId,
            )

        dao.insertPlaylistItems(
            newUris.mapIndexed {
                    offset,
                    uri ->
                PlaylistItemEntity(
                    id =
                        UUID.randomUUID()
                            .toString(),
                    playlistId =
                        playlistId,
                    mediaUri = uri,
                    position =
                        startPosition +
                                offset,
                    addedAtEpochMillis =
                        now,
                )
            },
        )

        if (touchPlaylist) {
            dao.getPlaylist(
                playlistId,
            )
                ?.let { playlist ->
                    dao.updatePlaylist(
                        playlist.copy(
                            updatedAtEpochMillis =
                                now,
                        ),
                    )
                }
        }

        return true
    }

    private suspend fun normalizePositions(
        playlistId: String,
    ) {
        val items =
            dao.getPlaylistItems(
                playlistId,
            )

        if (items.isEmpty()) {
            return
        }

        dao.updatePlaylistItems(
            items.mapIndexed {
                    index,
                    item ->
                item.copy(
                    position = index,
                )
            },
        )
    }

    private suspend fun buildUniqueName(
        baseName: String,
        exceptPlaylistId: String?,
    ): String {
        val existingNames =
            dao
                .getPlaylistNamesExcluding(
                    exceptPlaylistId
                        .orEmpty(),
                )
                .map {
                    it.lowercase()
                }
                .toHashSet()

        if (
            baseName.lowercase() !in
            existingNames
        ) {
            return baseName
        }

        var suffix = 2

        while (true) {
            val candidate =
                "$baseName $suffix"

            if (
                candidate.lowercase() !in
                existingNames
            ) {
                return candidate
            }

            suffix += 1
        }
    }

    private fun sanitizeCreateName(
        name: String,
    ): String {
        return name
            .trim()
            .ifEmpty {
                "New Playlist"
            }
    }

    private fun sanitizeRenameName(
        name: String,
    ): String? {
        return name
            .trim()
            .takeIf(
                String::isNotEmpty,
            )
    }
}
