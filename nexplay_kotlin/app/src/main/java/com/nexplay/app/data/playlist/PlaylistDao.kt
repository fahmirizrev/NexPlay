package com.nexplay.app.data.playlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT
            p.id AS id,
            p.name AS name,
            COUNT(i.id) AS itemCount,
            p.createdAtEpochMillis AS createdAtEpochMillis,
            p.updatedAtEpochMillis AS updatedAtEpochMillis
        FROM playlists AS p
        LEFT JOIN playlist_items AS i
            ON i.playlistId = p.id
        GROUP BY
            p.id,
            p.name,
            p.createdAtEpochMillis,
            p.updatedAtEpochMillis
        ORDER BY
            p.updatedAtEpochMillis DESC,
            p.name COLLATE NOCASE ASC
        """,
    )
    fun observePlaylistSummaries():
            Flow<List<PlaylistSummaryRow>>

    @Query(
        """
        SELECT *
        FROM playlists
        WHERE id = :playlistId
        LIMIT 1
        """,
    )
    fun observePlaylist(
        playlistId: String,
    ): Flow<PlaylistEntity?>

    @Query(
        """
        SELECT *
        FROM playlist_items
        WHERE playlistId = :playlistId
        ORDER BY position ASC
        """,
    )
    fun observePlaylistItems(
        playlistId: String,
    ): Flow<List<PlaylistItemEntity>>

    @Query(
        """
        SELECT *
        FROM playlists
        WHERE id = :playlistId
        LIMIT 1
        """,
    )
    suspend fun getPlaylist(
        playlistId: String,
    ): PlaylistEntity?

    @Query(
        """
        SELECT *
        FROM playlist_items
        WHERE playlistId = :playlistId
        ORDER BY position ASC
        """,
    )
    suspend fun getPlaylistItems(
        playlistId: String,
    ): List<PlaylistItemEntity>

    @Query(
        """
        SELECT mediaUri
        FROM playlist_items
        WHERE playlistId = :playlistId
        """,
    )
    suspend fun getMediaUris(
        playlistId: String,
    ): List<String>

    @Query(
        """
        SELECT COALESCE(MAX(position) + 1, 0)
        FROM playlist_items
        WHERE playlistId = :playlistId
        """,
    )
    suspend fun getNextPosition(
        playlistId: String,
    ): Int

    @Query(
        """
        SELECT name
        FROM playlists
        WHERE id != :exceptPlaylistId
        """,
    )
    suspend fun getPlaylistNamesExcluding(
        exceptPlaylistId: String,
    ): List<String>

    @Insert
    suspend fun insertPlaylist(
        playlist: PlaylistEntity,
    )

    @Insert(
        onConflict = OnConflictStrategy.IGNORE,
    )
    suspend fun insertPlaylistItems(
        items: List<PlaylistItemEntity>,
    )

    @Update
    suspend fun updatePlaylist(
        playlist: PlaylistEntity,
    )

    @Update
    suspend fun updatePlaylistItems(
        items: List<PlaylistItemEntity>,
    )

    @Query(
        """
        DELETE FROM playlists
        WHERE id = :playlistId
        """,
    )
    suspend fun deletePlaylist(
        playlistId: String,
    ): Int

    @Query(
        """
        DELETE FROM playlist_items
        WHERE playlistId = :playlistId
        """,
    )
    suspend fun clearPlaylistItems(
        playlistId: String,
    ): Int

    @Query(
        """
        DELETE FROM playlist_items
        WHERE id = :playlistItemId
        """,
    )
    suspend fun deletePlaylistItem(
        playlistItemId: String,
    ): Int
}
