package com.nexplay.app.data.media

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.nexplay.app.domain.model.AudioItem

sealed interface AudioMediaScanResult {
    data class Success(
        val items: List<AudioItem>,
    ) : AudioMediaScanResult

    object PermissionDenied : AudioMediaScanResult

    data class Failure(
        val cause: RuntimeException,
    ) : AudioMediaScanResult
}

object AudioMediaPermission {
    fun requiredPermission(
        sdkInt: Int,
    ): String {
        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun isGranted(
        context: Context,
    ): Boolean {
        val permission = requiredPermission(Build.VERSION.SDK_INT)

        return ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

class AudioMediaScanner(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    fun scan(): AudioMediaScanResult {
        if (!AudioMediaPermission.isGranted(applicationContext)) {
            return AudioMediaScanResult.PermissionDenied
        }

        return try {
            AudioMediaScanResult.Success(
                items = queryAudio(
                    contentResolver = applicationContext.contentResolver,
                ),
            )
        } catch (_: SecurityException) {
            AudioMediaScanResult.PermissionDenied
        } catch (error: RuntimeException) {
            AudioMediaScanResult.Failure(error)
        }
    }

    private fun queryAudio(
        contentResolver: ContentResolver,
    ): List<AudioItem> {
        val itemsById = linkedMapOf<String, AudioItem>()

        contentResolver.query(
            audioCollectionUri(),
            audioProjection(),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val item = readAudioItem(cursor) ?: continue
                itemsById[item.id] = item
            }
        }

        return itemsById.values.toList()
    }

    private fun readAudioItem(
        cursor: Cursor,
    ): AudioItem? {
        val rawId =
            cursor.longOrNull(MediaStore.Audio.Media._ID)
                ?.takeIf { it > 0L }
                ?: return null

        val volumeName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor
                    .stringOrNull(MediaStore.MediaColumns.VOLUME_NAME)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: return null
            } else {
                null
            }

        val itemUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentUris.withAppendedId(
                    MediaStore.Audio.Media.getContentUri(volumeName!!),
                    rawId,
                )
            } else {
                ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    rawId,
                )
            }

        val stableId =
            if (volumeName != null) {
                "$volumeName:$rawId"
            } else {
                rawId.toString()
            }

        val folder = resolveFolder(cursor)

        return AudioMediaMapper.map(
            AudioMediaRow(
                id = stableId,
                uri = itemUri.toString(),
                displayName =
                    cursor.stringOrNull(
                        MediaStore.Audio.Media.DISPLAY_NAME,
                    ),
                title =
                    cursor.stringOrNull(
                        MediaStore.Audio.Media.TITLE,
                    ),
                durationMs =
                    cursor.longOrNull(
                        MediaStore.Audio.Media.DURATION,
                    ),
                mimeType =
                    cursor.stringOrNull(
                        MediaStore.Audio.Media.MIME_TYPE,
                    ),
                folderName = folder.first,
                folderPath = folder.second,
                sizeBytes =
                    cursor.longOrNull(
                        MediaStore.Audio.Media.SIZE,
                    ),
                dateAddedEpochSeconds =
                    cursor.longOrNull(
                        MediaStore.Audio.Media.DATE_ADDED,
                    ),
                dateModifiedEpochSeconds =
                    cursor.longOrNull(
                        MediaStore.Audio.Media.DATE_MODIFIED,
                    ),
                artist =
                    cursor.stringOrNull(
                        MediaStore.Audio.Media.ARTIST,
                    ),
                album =
                    cursor.stringOrNull(
                        MediaStore.Audio.Media.ALBUM,
                    ),
                albumId =
                    cursor
                        .longOrNull(MediaStore.Audio.Media.ALBUM_ID)
                        ?.takeIf { it > 0L }
                        ?.toString(),
                track =
                    cursor.intOrNull(
                        MediaStore.Audio.Media.TRACK,
                    ),
            ),
        )
    }

    private fun resolveFolder(
        cursor: Cursor,
    ): Pair<String?, String?> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath =
                cursor
                    .stringOrNull(MediaStore.MediaColumns.RELATIVE_PATH)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            return folderNameFromPath(relativePath) to relativePath
        }

        val dataPath =
            cursor
                .stringOrNull(legacyDataColumn())
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        val parentPath =
            dataPath
                ?.substringBeforeLast(
                    delimiter = "/",
                    missingDelimiterValue = "",
                )
                ?.takeIf { it.isNotEmpty() }

        return folderNameFromPath(parentPath) to parentPath
    }

    private fun folderNameFromPath(
        path: String?,
    ): String? {
        return path
            ?.trim()
            ?.trimEnd('/')
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotEmpty() }
    }

    private fun audioCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL,
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun audioProjection(): Array<String> {
        val columns =
            mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK,
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns += MediaStore.MediaColumns.RELATIVE_PATH
            columns += MediaStore.MediaColumns.VOLUME_NAME
        } else {
            columns += legacyDataColumn()
        }

        return columns.toTypedArray()
    }

    @Suppress("DEPRECATION")
    private fun legacyDataColumn(): String {
        return MediaStore.MediaColumns.DATA
    }

    private fun Cursor.stringOrNull(
        columnName: String,
    ): String? {
        val index = getColumnIndex(columnName)

        if (index < 0 || isNull(index)) {
            return null
        }

        return runCatching {
            getString(index)
        }.getOrNull()
    }

    private fun Cursor.longOrNull(
        columnName: String,
    ): Long? {
        val index = getColumnIndex(columnName)

        if (index < 0 || isNull(index)) {
            return null
        }

        return runCatching {
            getLong(index)
        }.getOrNull()
    }

    private fun Cursor.intOrNull(
        columnName: String,
    ): Int? {
        val index = getColumnIndex(columnName)

        if (index < 0 || isNull(index)) {
            return null
        }

        return runCatching {
            getInt(index)
        }.getOrNull()
    }
}