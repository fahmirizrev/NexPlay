package com.nexplay.app.data.playlist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NexPlayDatabase :
    RoomDatabase() {
    abstract fun playlistDao():
            PlaylistDao

    companion object {
        fun create(
            context: Context,
            databaseName: String =
                "nexplay.db",
        ): NexPlayDatabase {
            return Room
                .databaseBuilder(
                    context.applicationContext,
                    NexPlayDatabase::class.java,
                    databaseName,
                )
                .build()
        }
    }
}
