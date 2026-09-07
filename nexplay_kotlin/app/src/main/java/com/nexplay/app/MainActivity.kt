package com.nexplay.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.MediaType
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.ui.NexPlayApp
import com.nexplay.app.ui.theme.NexPlayDarkBackground
import com.nexplay.app.ui.theme.NexPlayLightBackground
import com.nexplay.app.ui.theme.NexPlayTheme
import com.nexplay.app.ui.theme.NexPlayThemeMode
import com.nexplay.app.ui.theme.resolveThemeMode
import kotlinx.coroutines.launch

internal data class ExternalMediaOpenRequest(
    val requestId: Long,
    val item: MediaItem,
)

class MainActivity : ComponentActivity() {
    private val externalMediaRequest =
        mutableStateOf<ExternalMediaOpenRequest?>(
            null,
        )

    private var nextExternalMediaRequestId =
        0L

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(
            R.style.Theme_NexPlay,
        )

        super.onCreate(savedInstanceState)

        configureEdgeToEdge(
            darkTheme = true,
        )

        publishExternalMediaIntent(
            intent,
        )

        val preferencesRepository =
            (application as NexPlayApplication)
                .preferencesRepository

        setContent {
            val preferences by
                preferencesRepository
                    .preferences
                    .collectAsState(
                        initial = null,
                    )

            val resolvedPreferences =
                preferences
                    ?: return@setContent

            val themeMode =
                resolvedPreferences
                    .resolveThemeMode()

            val scope =
                rememberCoroutineScope()

            val systemDarkTheme =
                isSystemInDarkTheme()

            val darkTheme =
                when (themeMode) {
                    NexPlayThemeMode.SYSTEM ->
                        systemDarkTheme

                    NexPlayThemeMode.LIGHT ->
                        false

                    NexPlayThemeMode.DARK ->
                        true
                }

            SideEffect {
                configureEdgeToEdge(
                    darkTheme = darkTheme,
                )
            }

            NexPlayTheme(
                darkTheme = darkTheme,
            ) {
                NexPlayApp(
                    themeMode = themeMode,
                    onThemeModeSelected = {
                            nextThemeMode ->
                        scope.launch {
                            preferencesRepository
                                .setThemeMode(
                                    nextThemeMode.name,
                                )
                        }
                    },
                    externalMediaRequest =
                        externalMediaRequest.value,
                    onExternalMediaConsumed =
                        ::consumeExternalMediaRequest,
                )
            }
        }
    }

    override fun onNewIntent(
        intent: Intent,
    ) {
        super.onNewIntent(
            intent,
        )

        setIntent(
            intent,
        )

        publishExternalMediaIntent(
            intent,
        )
    }

    private fun publishExternalMediaIntent(
        sourceIntent: Intent?,
    ) {
        val resolvedIntent =
            sourceIntent
                ?: return

        val item =
            resolveExternalMediaItem(
                resolvedIntent,
            )
                ?: return

        nextExternalMediaRequestId += 1L

        externalMediaRequest.value =
            ExternalMediaOpenRequest(
                requestId =
                    nextExternalMediaRequestId,
                item = item,
            )

        setIntent(
            Intent(
                resolvedIntent,
            ).apply {
                action = null
                data = null
                type = null
            },
        )
    }

    private fun consumeExternalMediaRequest(
        requestId: Long,
    ) {
        if (
            externalMediaRequest
                .value
                ?.requestId !=
            requestId
        ) {
            return
        }

        externalMediaRequest.value =
            null
    }

    private fun resolveExternalMediaItem(
        sourceIntent: Intent,
    ): MediaItem? {
        if (
            sourceIntent.action !=
            Intent.ACTION_VIEW
        ) {
            return null
        }

        val uri =
            sourceIntent.data
                ?: return null

        val displayName =
            readExternalDisplayName(
                uri,
            )
                ?: "External media"

        val mimeType =
            sourceIntent
                .type
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )
                ?: try {
                    contentResolver
                        .getType(
                            uri,
                        )
                        ?.trim()
                        ?.takeIf(
                            String::isNotEmpty,
                        )
                } catch (_: RuntimeException) {
                    null
                }

        val mediaType =
            resolveExternalMediaType(
                uri = uri,
                mimeType = mimeType,
                displayName =
                    displayName,
            )
                ?: return null

        val title =
            externalMediaTitle(
                displayName,
            )

        val id =
            "external:" +
                mediaType
                    .name
                    .lowercase() +
                ":" +
                uri

        return when (mediaType) {
            MediaType.AUDIO ->
                AudioItem(
                    id = id,
                    uri = uri.toString(),
                    displayName =
                        displayName,
                    title = title,
                    durationMs = null,
                    mimeType = mimeType,
                    folder = null,
                    sizeBytes = null,
                    dateAddedEpochSeconds =
                        null,
                    dateModifiedEpochSeconds =
                        null,
                    artist = null,
                    album = null,
                    albumId = null,
                    track = null,
                )

            MediaType.VIDEO ->
                VideoItem(
                    id = id,
                    uri = uri.toString(),
                    displayName =
                        displayName,
                    title = title,
                    durationMs = null,
                    mimeType = mimeType,
                    folder = null,
                    sizeBytes = null,
                    dateAddedEpochSeconds =
                        null,
                    dateModifiedEpochSeconds =
                        null,
                    width = null,
                    height = null,
                )
        }
    }

    private fun resolveExternalMediaType(
        uri: Uri,
        mimeType: String?,
        displayName: String,
    ): MediaType? {
        val normalizedMimeType =
            mimeType
                ?.lowercase()

        if (
            normalizedMimeType
                ?.startsWith(
                    "audio/",
                ) == true
        ) {
            return MediaType.AUDIO
        }

        if (
            normalizedMimeType
                ?.startsWith(
                    "video/",
                ) == true
        ) {
            return MediaType.VIDEO
        }

        val fallbackName =
            if (
                displayName !=
                "External media"
            ) {
                displayName
            } else {
                Uri.decode(
                    uri.lastPathSegment
                        .orEmpty(),
                )
            }
                .lowercase()

        return when {
            fallbackName.endsWith(".mp3") ||
                fallbackName.endsWith(".m4a") ||
                fallbackName.endsWith(".aac") ||
                fallbackName.endsWith(".wav") ||
                fallbackName.endsWith(".ogg") ||
                fallbackName.endsWith(".flac") ->
                MediaType.AUDIO

            fallbackName.endsWith(".mp4") ||
                fallbackName.endsWith(".mkv") ||
                fallbackName.endsWith(".webm") ||
                fallbackName.endsWith(".3gp") ||
                fallbackName.endsWith(".avi") ||
                fallbackName.endsWith(".mov") ->
                MediaType.VIDEO

            else ->
                null
        }
    }

    private fun readExternalDisplayName(
        uri: Uri,
    ): String? {
        val queriedName =
            try {
                contentResolver
                    .query(
                        uri,
                        arrayOf(
                            OpenableColumns
                                .DISPLAY_NAME,
                        ),
                        null,
                        null,
                        null,
                    )
                    ?.use { cursor ->
                        val nameColumn =
                            cursor.getColumnIndex(
                                OpenableColumns
                                    .DISPLAY_NAME,
                            )

                        if (
                            nameColumn >= 0 &&
                            cursor.moveToFirst()
                        ) {
                            cursor
                                .getString(
                                    nameColumn,
                                )
                                ?.trim()
                                ?.takeIf(
                                    String::isNotEmpty,
                                )
                        } else {
                            null
                        }
                    }
            } catch (_: RuntimeException) {
                null
            }

        if (queriedName != null) {
            return queriedName
        }

        return Uri.decode(
            uri.lastPathSegment
                .orEmpty(),
        )
            .trim()
            .takeIf(
                String::isNotEmpty,
            )
    }

    private fun externalMediaTitle(
        displayName: String,
    ): String {
        val dotIndex =
            displayName
                .lastIndexOf('.')

        return if (dotIndex > 0) {
            displayName.substring(
                0,
                dotIndex,
            )
        } else {
            displayName
        }
    }

    private fun configureEdgeToEdge(
        darkTheme: Boolean,
    ) {
        val backgroundColor =
            if (darkTheme) {
                NexPlayDarkBackground.toArgb()
            } else {
                NexPlayLightBackground.toArgb()
            }

        val systemBarStyle =
            if (darkTheme) {
                SystemBarStyle.dark(
                    backgroundColor,
                )
            } else {
                SystemBarStyle.light(
                    scrim = backgroundColor,
                    darkScrim =
                        NexPlayDarkBackground.toArgb(),
                )
            }

        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle,
        )
    }
}
