package com.nexplay.app.data.media

import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.FolderInfo

internal data class AudioMediaRow(
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
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val track: Int?,
)

internal object AudioMediaMapper {
    fun map(row: AudioMediaRow): AudioItem? {
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

        return AudioItem(
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
            artist = normalizeMetadata(row.artist),
            album = normalizeMetadata(row.album),
            albumId = normalizeText(row.albumId),
            track = row.track?.takeIf { it > 0 },
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
}