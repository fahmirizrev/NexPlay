package com.nexplay.app.data.media

import com.nexplay.app.domain.model.FolderInfo
import com.nexplay.app.domain.model.VideoItem

internal data class VideoMediaRow(
    val id: String?,
    val uri: String?,
    val displayName: String?,
    val title: String?,
    val durationMs: Long?,
    val mimeType: String?,
    val folderName: String?,
    val folderPath: String?,
    val sizeBytes: Long?,
    val dateAddedEpochSeconds: Long?,
    val dateModifiedEpochSeconds: Long?,
    val width: Int?,
    val height: Int?,
)

internal object VideoMediaMapper {
    fun map(row: VideoMediaRow): VideoItem? {
        val id = normalizeText(row.id) ?: return null
        val uri = normalizeText(row.uri) ?: return null

        val displayName =
            normalizeText(row.displayName)
                ?: normalizeMetadata(row.title)
                ?: return null

        val title =
            normalizeMetadata(row.title)
                ?: displayName
                    .substringBeforeLast(
                        delimiter = ".",
                        missingDelimiterValue = displayName,
                    )
                    .ifBlank { displayName }

        val folderPath = normalizeText(row.folderPath)
        val folderName =
            normalizeText(row.folderName)
                ?: folderNameFromPath(folderPath)

        val folder = folderName?.let {
            FolderInfo(
                name = it,
                path = folderPath,
            )
        }

        return VideoItem(
            id = id,
            uri = uri,
            displayName = displayName,
            title = title,
            durationMs = row.durationMs.positiveOrNull(),
            mimeType = normalizeText(row.mimeType),
            folder = folder,
            sizeBytes = row.sizeBytes.positiveOrNull(),
            dateAddedEpochSeconds = row.dateAddedEpochSeconds.positiveOrNull(),
            dateModifiedEpochSeconds = row.dateModifiedEpochSeconds.positiveOrNull(),
            width = row.width.positiveOrNull(),
            height = row.height.positiveOrNull(),
        )
    }

    private fun normalizeText(value: String?): String? {
        return value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun normalizeMetadata(value: String?): String? {
        return normalizeText(value)
            ?.takeUnless { it.equals("<unknown>", ignoreCase = true) }
    }

    private fun folderNameFromPath(path: String?): String? {
        val normalizedPath =
            path
                ?.trim()
                ?.trimEnd('/', '\\')
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        return normalizedPath
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .takeIf { it.isNotEmpty() }
    }

    private fun Long?.positiveOrNull(): Long? {
        return this?.takeIf { it > 0L }
    }

    private fun Int?.positiveOrNull(): Int? {
        return this?.takeIf { it > 0 }
    }
}