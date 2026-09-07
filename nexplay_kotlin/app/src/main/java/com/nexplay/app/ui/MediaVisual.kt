package com.nexplay.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexplay.app.data.media.MediaVisualLoader
import com.nexplay.app.domain.model.MediaItem

@Composable
internal fun MediaVisualBox(
    item: MediaItem,
    size: Dp,
    cornerRadius: Dp,
    fallback:
    @Composable () -> Unit,
    modifier: Modifier = Modifier,
    highQualityAudioArtwork: Boolean =
        false,
) {
    val context =
        LocalContext.current

    val density =
        LocalDensity.current

    val targetPixelSize =
        with(density) {
            size.roundToPx()
        }
            .coerceAtLeast(1)

    var bitmap by
    remember(
        item.mediaType,
        item.uri,
        item.dateModifiedEpochSeconds,
        targetPixelSize,
        highQualityAudioArtwork,
    ) {
        mutableStateOf(
            MediaVisualLoader.cached(
                item = item,
                targetPixelSize =
                    targetPixelSize,
                highQualityAudioArtwork =
                    highQualityAudioArtwork,
            ),
        )
    }

    LaunchedEffect(
        item.mediaType,
        item.uri,
        item.dateModifiedEpochSeconds,
        targetPixelSize,
        highQualityAudioArtwork,
    ) {
        if (bitmap == null) {
            bitmap =
                MediaVisualLoader.load(
                    context = context,
                    item = item,
                    targetPixelSize =
                        targetPixelSize,
                    highQualityAudioArtwork =
                        highQualityAudioArtwork,
                )
        }
    }

    val imageBitmap =
        remember(bitmap) {
            bitmap
                ?.asImageBitmap()
        }

    Box(
        modifier =
            modifier
                .size(size)
                .clip(
                    RoundedCornerShape(
                        cornerRadius,
                    ),
                ),
        contentAlignment =
            Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier =
                    Modifier.fillMaxSize(),
                contentScale =
                    ContentScale.Crop,
            )
        } else {
            fallback()
        }
    }
}

@Composable
internal fun MediaThumbnailBox(
    item: MediaItem,
    size: Dp,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    MediaVisualBox(
        item = item,
        size = size,
        cornerRadius =
            cornerRadius,
        modifier = modifier,
        fallback = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant,
                        ),
                contentAlignment =
                    Alignment.Center,
            ) {
                Icon(
                    imageVector =
                        fallbackIcon,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            NexPlaySpacing
                                .listLeadingIconSize,
                        ),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onBackground,
                )
            }
        },
    )
}