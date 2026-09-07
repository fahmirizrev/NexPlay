package com.nexplay.app.data.playlist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlists",
)
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["playlistId"],
        ),
        Index(
            value = [
                "playlistId",
                "mediaUri",
            ],
            unique = true,
        ),
    ],
)
data class PlaylistItemEntity(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val mediaUri: String,
    val position: Int,
    val addedAtEpochMillis: Long,
)

data class PlaylistSummaryRow(
    val id: String,
    val name: String,
    val itemCount: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
