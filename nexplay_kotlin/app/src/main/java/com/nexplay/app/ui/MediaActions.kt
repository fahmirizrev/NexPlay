package com.nexplay.app.ui

import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal fun shareMedia(
    context: Context,
    item: MediaItem,
) {
    val uri =
        runCatching {
            Uri.parse(
                item.uri,
            )
        }
            .getOrNull()

    if (
        uri == null ||
        uri.scheme !=
        ContentResolver.SCHEME_CONTENT
    ) {
        Toast.makeText(
            context,
            "Sharing is unavailable for this media source.",
            Toast.LENGTH_SHORT,
        ).show()

        return
    }

    val mimeType =
        item.mimeType
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?: when (item) {
                is AudioItem -> "audio/*"
                is VideoItem -> "video/*"
            }

    val shareIntent =
        Intent(
            Intent.ACTION_SEND,
        ).apply {
            type = mimeType
            putExtra(
                Intent.EXTRA_STREAM,
                uri,
            )
            clipData =
                ClipData.newUri(
                    context.contentResolver,
                    item.displayName,
                    uri,
                )
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

    runCatching {
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                "Share media",
            ),
        )
    }
        .onFailure {
            Toast.makeText(
                context,
                "No app is available to share this media.",
                Toast.LENGTH_SHORT,
            ).show()
        }
}

@Composable
internal fun MediaDetailsDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val context =
        LocalContext.current

    val rows =
        remember(
            item,
            context,
        ) {
            buildMediaDetailRows(
                context = context,
                item = item,
            )
        }

    val listState =
        rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Details",
            )
        },
        text = {
            Box {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 440.dp,
                            ),
                ) {
                    items(
                        items = rows,
                        key = {
                            it.first
                        },
                    ) {
                            row ->
                        Column {
                            Text(
                                text = row.first,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        2.dp,
                                    ),
                            )

                            Text(
                                text = row.second,
                                maxLines =
                                    if (
                                        row.first == "URI"
                                    ) {
                                        3
                                    } else {
                                        2
                                    },
                                overflow =
                                    TextOverflow.Ellipsis,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp,
                                    ),
                            )
                        }
                    }
                }

                NexPlayScrollbar(
                    listState = listState,
                    modifier =
                        Modifier.align(
                            androidx.compose.ui.Alignment.CenterEnd,
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "Close",
                )
            }
        },
    )
}

private fun buildMediaDetailRows(
    context: Context,
    item: MediaItem,
): List<Pair<String, String>> {
    val rows =
        mutableListOf<Pair<String, String>>()

    rows +=
        "Title" to
                item.title.ifBlank {
                    item.displayName
                }

    rows +=
        "File name" to
                item.displayName

    rows +=
        "Type" to
                when (item) {
                    is AudioItem -> "Audio"
                    is VideoItem -> "Video"
                }

    when (item) {
        is AudioItem -> {
            rows +=
                "Artist" to
                        item.artist.orUnknown()
            rows +=
                "Album" to
                        item.album.orUnknown()
            rows +=
                "Track" to
                        item.track
                            ?.toString()
                            .orUnknown()
        }

        is VideoItem -> {
            rows +=
                "Resolution" to
                        item.resolution
                            .orUnknown()
        }
    }

    rows +=
        "Duration" to
                formatMediaDetailDuration(
                    item.durationMs,
                )

    rows +=
        "Size" to
                item.sizeBytes
                    ?.takeIf {
                        it >= 0L
                    }
                    ?.let { size ->
                        Formatter.formatFileSize(
                            context,
                            size,
                        )
                    }
                    .orUnknown()

    rows +=
        "MIME type" to
                item.mimeType.orUnknown()

    rows +=
        "Folder" to
                item.folder
                    ?.name
                    .orUnknown()

    rows +=
        "Date added" to
                formatMediaDetailDate(
                    item.dateAddedEpochSeconds,
                )

    rows +=
        "Date modified" to
                formatMediaDetailDate(
                    item.dateModifiedEpochSeconds,
                )

    rows +=
        "URI" to item.uri

    return rows
}

private fun formatMediaDetailDuration(
    durationMs: Long?,
): String {
    val resolvedDuration =
        durationMs
            ?.takeIf {
                it >= 0L
            }
            ?: return "Unknown"

    val totalSeconds =
        resolvedDuration /
                1_000L

    val hours =
        totalSeconds /
                3_600L

    val minutes =
        (
                totalSeconds %
                        3_600L
                ) /
                60L

    val seconds =
        totalSeconds %
                60L

    return if (hours > 0L) {
        String.format(
            Locale.US,
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds,
        )
    } else {
        String.format(
            Locale.US,
            "%d:%02d",
            minutes,
            seconds,
        )
    }
}

private fun formatMediaDetailDate(
    epochSeconds: Long?,
): String {
    val resolvedEpochSeconds =
        epochSeconds
            ?.takeIf {
                it > 0L
            }
            ?: return "Unknown"

    return DateFormat
        .getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
        )
        .format(
            Date(
                resolvedEpochSeconds *
                        1_000L,
            ),
        )
}

private fun String?.orUnknown(): String {
    return this
        ?.trim()
        ?.takeIf {
            it.isNotEmpty()
        }
        ?: "Unknown"
}
