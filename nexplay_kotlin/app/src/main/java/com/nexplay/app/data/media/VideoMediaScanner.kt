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
import com.nexplay.app.domain.model.VideoItem

sealed interface VideoMediaScanResult {
    data class Success(
        val items: List<VideoItem>,
    ) : VideoMediaScanResult

    object PermissionDenied : VideoMediaScanResult

    data class Failure(
        val cause: RuntimeException,
    ) : VideoMediaScanResult
}

object VideoMediaPermission {
    fun requiredPermission(
        sdkInt: Int,
    ): String {
        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
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

class VideoMediaScanner(
    context: Context,
) {
    private val applicationContext = context.applicationContext

    fun scan(): VideoMediaScanResult {
        if (!VideoMediaPermission.isGranted(applicationContext)) {
            return VideoMediaScanResult.PermissionDenied
        }

        return try {
            VideoMediaScanResult.Success(
                items = queryVideo(
                    contentResolver = applicationContext.contentResolver,
                ),
            )
        } catch (_: SecurityException) {
            VideoMediaScanResult.PermissionDenied
        } catch (error: RuntimeException) {
            VideoMediaScanResult.Failure(error)
        }
    }

    private fun queryVideo(
        contentResolver: ContentResolver,
    ): List<VideoItem> {
        val itemsById = linkedMapOf<String, VideoItem>()

        contentResolver.query(
            videoCollectionUri(),
            videoProjection(),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val item = readVideoItem(cursor) ?: continue
                itemsById[item.id] = item
            }
        }

        return itemsById.values.toList()
    }

    private fun readVideoItem(
        cursor: Cursor,
    ): VideoItem? {
        val rawId =
            cursor.longOrNull(MediaStore.Video.Media._ID)
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
                    MediaStore.Video.Media.getContentUri(volumeName!!),
                    rawId,
                )
            } else {
                ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
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

        return VideoMediaMapper.map(
            VideoMediaRow(
                id = stableId,
                uri = itemUri.toString(),
                displayName =
                    cursor.stringOrNull(
                        MediaStore.Video.Media.DISPLAY_NAME,
                    ),
                title =
                    cursor.stringOrNull(
                        MediaStore.Video.Media.TITLE,
                    ),
                durationMs =
                    cursor.longOrNull(
                        MediaStore.Video.Media.DURATION,
                    ),
                mimeType =
                    cursor.stringOrNull(
                        MediaStore.Video.Media.MIME_TYPE,
                    ),
                folderName = folder.first,
                folderPath = folder.second,
                sizeBytes =
                    cursor.longOrNull(
                        MediaStore.Video.Media.SIZE,
                    ),
                dateAddedEpochSeconds =
                    cursor.longOrNull(
                        MediaStore.Video.Media.DATE_ADDED,
                    ),
                dateModifiedEpochSeconds =
                    cursor.longOrNull(
                        MediaStore.Video.Media.DATE_MODIFIED,
                    ),
                width =
                    cursor.intOrNull(
                        MediaStore.Video.Media.WIDTH,
                    ),
                height =
                    cursor.intOrNull(
                        MediaStore.Video.Media.HEIGHT,
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

    private fun videoCollectionUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL,
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun videoProjection(): Array<String> {
        val columns =
            mutableListOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
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