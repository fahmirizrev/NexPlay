package com.nexplay.app.data.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object MediaVisualLoader {
    private const val CacheSizeKilobytes =
        8 * 1024

    private const val MaxFailedKeys =
        256

    private const val MaxConcurrentLoads =
        2

    private const val MinTargetPixelSize =
        40

    private const val MaxTargetPixelSize =
        768

    private const val MaxHighQualityAudioArtworkPixelSize =
        1024

    private val bitmapCache =
        object :
            LruCache<String, Bitmap>(
                CacheSizeKilobytes,
            ) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int {
                return max(
                    1,
                    value.allocationByteCount /
                            1024,
                )
            }
        }

    private val failedKeys =
        LinkedHashSet<String>()

    private val loadSemaphore =
        Semaphore(
            MaxConcurrentLoads,
        )

    fun cached(
        item: MediaItem,
        targetPixelSize: Int,
        highQualityAudioArtwork: Boolean =
            false,
    ): Bitmap? {
        val normalizedTarget =
            normalizeTargetSize(
                targetPixelSize =
                    targetPixelSize,
                highQualityAudioArtwork =
                    highQualityAudioArtwork,
            )

        return bitmapCache.get(
            cacheKey(
                item = item,
                targetPixelSize =
                    normalizedTarget,
                highQualityAudioArtwork =
                    highQualityAudioArtwork,
            ),
        )
    }

    suspend fun load(
        context: Context,
        item: MediaItem,
        targetPixelSize: Int,
        highQualityAudioArtwork: Boolean =
            false,
    ): Bitmap? {
        val applicationContext =
            context.applicationContext

        val normalizedTarget =
            normalizeTargetSize(
                targetPixelSize =
                    targetPixelSize,
                highQualityAudioArtwork =
                    highQualityAudioArtwork,
            )

        val cacheKey =
            cacheKey(
                item = item,
                targetPixelSize =
                    normalizedTarget,
                highQualityAudioArtwork =
                    highQualityAudioArtwork,
            )

        val failureKey =
            failureKey(item)

        bitmapCache
            .get(cacheKey)
            ?.let { bitmap ->
                return bitmap
            }

        if (isFailed(failureKey)) {
            return null
        }

        return loadSemaphore.withPermit {
            bitmapCache
                .get(cacheKey)
                ?.let { bitmap ->
                    return@withPermit bitmap
                }

            if (isFailed(failureKey)) {
                return@withPermit null
            }

            val bitmap =
                withContext(
                    Dispatchers.IO,
                ) {
                    loadBitmap(
                        context =
                            applicationContext,
                        item = item,
                        targetPixelSize =
                            normalizedTarget,
                        highQualityAudioArtwork =
                            highQualityAudioArtwork,
                    )
                }

            if (bitmap == null) {
                rememberFailure(
                    failureKey,
                )

                return@withPermit null
            }

            bitmapCache.put(
                cacheKey,
                bitmap,
            )

            bitmap
        }
    }

    fun clear() {
        bitmapCache.evictAll()

        synchronized(failedKeys) {
            failedKeys.clear()
        }
    }

    private fun loadBitmap(
        context: Context,
        item: MediaItem,
        targetPixelSize: Int,
        highQualityAudioArtwork: Boolean,
    ): Bitmap? {
        if (
            highQualityAudioArtwork &&
            item is AudioItem
        ) {
            loadAudioArtwork(
                context = context,
                item = item,
                targetPixelSize =
                    targetPixelSize,
            )
                ?.let { bitmap ->
                    return bitmap
                }
        }

        val resolvedUri =
            resolveUri(item.uri)

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q &&
            resolvedUri?.scheme ==
            ContentResolver.SCHEME_CONTENT
        ) {
            val providerThumbnail =
                runCatching {
                    context
                        .contentResolver
                        .loadThumbnail(
                            resolvedUri,
                            Size(
                                targetPixelSize,
                                targetPixelSize,
                            ),
                            null,
                        )
                }
                    .getOrNull()

            if (providerThumbnail != null) {
                return normalizeBitmap(
                    bitmap =
                        providerThumbnail,
                    targetPixelSize =
                        targetPixelSize,
                )
            }
        }

        return when (item) {
            is AudioItem ->
                if (highQualityAudioArtwork) {
                    null
                } else {
                    loadAudioArtwork(
                        context = context,
                        item = item,
                        targetPixelSize =
                            targetPixelSize,
                    )
                }

            is VideoItem ->
                loadVideoThumbnail(
                    context = context,
                    item = item,
                    targetPixelSize =
                        targetPixelSize,
                )
        }
    }

    private fun loadAudioArtwork(
        context: Context,
        item: AudioItem,
        targetPixelSize: Int,
    ): Bitmap? {
        val retriever =
            createRetriever(
                context = context,
                uriValue = item.uri,
            )
                ?: return null

        return try {
            val embeddedPicture =
                runCatching {
                    retriever.embeddedPicture
                }
                    .getOrNull()
                    ?: return null

            decodeSampledBitmap(
                bytes = embeddedPicture,
                targetPixelSize =
                    targetPixelSize,
            )
        } finally {
            runCatching {
                retriever.release()
            }
        }
    }

    private fun loadVideoThumbnail(
        context: Context,
        item: VideoItem,
        targetPixelSize: Int,
    ): Bitmap? {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {
            loadLegacyVideoThumbnail(
                context = context,
                item = item,
                targetPixelSize =
                    targetPixelSize,
            )
                ?.let { bitmap ->
                    return bitmap
                }
        }

        return loadVideoFrame(
            context = context,
            item = item,
            targetPixelSize =
                targetPixelSize,
        )
    }

    @Suppress("DEPRECATION")
    private fun loadLegacyVideoThumbnail(
        context: Context,
        item: VideoItem,
        targetPixelSize: Int,
    ): Bitmap? {
        val mediaId =
            item.id
                .toLongOrNull()
                ?: return null

        val bitmap =
            runCatching {
                MediaStore
                    .Video
                    .Thumbnails
                    .getThumbnail(
                        context.contentResolver,
                        mediaId,
                        MediaStore
                            .Video
                            .Thumbnails
                            .MINI_KIND,
                        null,
                    )
            }
                .getOrNull()
                ?: return null

        return normalizeBitmap(
            bitmap = bitmap,
            targetPixelSize =
                targetPixelSize,
        )
    }

    private fun loadVideoFrame(
        context: Context,
        item: VideoItem,
        targetPixelSize: Int,
    ): Bitmap? {
        val retriever =
            createRetriever(
                context = context,
                uriValue = item.uri,
            )
                ?: return null

        return try {
            val frameTimeUs =
                resolveVideoFrameTimeUs(
                    item,
                )

            val bitmap =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O_MR1
                ) {
                    val frameSize =
                        resolveVideoFrameSize(
                            item = item,
                            targetPixelSize =
                                targetPixelSize,
                        )

                    runCatching {
                        retriever
                            .getScaledFrameAtTime(
                                frameTimeUs,
                                MediaMetadataRetriever
                                    .OPTION_CLOSEST_SYNC,
                                frameSize.first,
                                frameSize.second,
                            )
                    }
                        .getOrNull()
                        ?: runCatching {
                            retriever
                                .getFrameAtTime(
                                    frameTimeUs,
                                    MediaMetadataRetriever
                                        .OPTION_CLOSEST_SYNC,
                                )
                        }
                            .getOrNull()
                } else {
                    runCatching {
                        retriever
                            .getFrameAtTime(
                                frameTimeUs,
                                MediaMetadataRetriever
                                    .OPTION_CLOSEST_SYNC,
                            )
                    }
                        .getOrNull()
                }

            bitmap
                ?.let { resolvedBitmap ->
                    normalizeBitmap(
                        bitmap =
                            resolvedBitmap,
                        targetPixelSize =
                            targetPixelSize,
                    )
                }
        } finally {
            runCatching {
                retriever.release()
            }
        }
    }

    private fun createRetriever(
        context: Context,
        uriValue: String,
    ): MediaMetadataRetriever? {
        val trimmedUri =
            uriValue
                .trim()

        if (trimmedUri.isEmpty()) {
            return null
        }

        val retriever =
            MediaMetadataRetriever()

        return try {
            val uri =
                resolveUri(
                    trimmedUri,
                )

            when {
                uri == null ||
                        uri.scheme.isNullOrEmpty() -> {
                    retriever.setDataSource(
                        trimmedUri,
                    )
                }

                uri.scheme.equals(
                    ContentResolver
                        .SCHEME_FILE,
                    ignoreCase = true,
                ) -> {
                    retriever.setDataSource(
                        uri.path
                            ?: trimmedUri,
                    )
                }

                else -> {
                    retriever.setDataSource(
                        context,
                        uri,
                    )
                }
            }

            retriever
        } catch (_: RuntimeException) {
            runCatching {
                retriever.release()
            }

            null
        }
    }

    private fun resolveUri(
        uriValue: String,
    ): Uri? {
        val trimmedUri =
            uriValue.trim()

        if (trimmedUri.isEmpty()) {
            return null
        }

        return runCatching {
            Uri.parse(
                trimmedUri,
            )
        }
            .getOrNull()
    }

    private fun decodeSampledBitmap(
        bytes: ByteArray,
        targetPixelSize: Int,
    ): Bitmap? {
        if (bytes.isEmpty()) {
            return null
        }

        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            boundsOptions,
        )

        val sourceWidth =
            boundsOptions.outWidth

        val sourceHeight =
            boundsOptions.outHeight

        if (
            sourceWidth <= 0 ||
            sourceHeight <= 0
        ) {
            return null
        }

        var sampleSize = 1

        while (true) {
            val nextSample =
                sampleSize * 2

            val nextWidth =
                sourceWidth /
                        nextSample

            val nextHeight =
                sourceHeight /
                        nextSample

            if (
                min(
                    nextWidth,
                    nextHeight,
                ) <
                targetPixelSize
            ) {
                break
            }

            sampleSize =
                nextSample
        }

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize =
                    sampleSize
            }

        val decodedBitmap =
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                decodeOptions,
            )
                ?: return null

        return normalizeBitmap(
            bitmap = decodedBitmap,
            targetPixelSize =
                targetPixelSize,
        )
    }

    private fun normalizeBitmap(
        bitmap: Bitmap,
        targetPixelSize: Int,
    ): Bitmap {
        val sourceWidth =
            bitmap.width

        val sourceHeight =
            bitmap.height

        if (
            sourceWidth <= 0 ||
            sourceHeight <= 0
        ) {
            return bitmap
        }

        val shortSide =
            min(
                sourceWidth,
                sourceHeight,
            )

        if (
            shortSide <=
            targetPixelSize
        ) {
            return bitmap
        }

        val scale =
            targetPixelSize
                .toFloat() /
                    shortSide
                        .toFloat()

        val targetWidth =
            (
                    sourceWidth *
                            scale
                    )
                .roundToInt()
                .coerceAtLeast(1)

        val targetHeight =
            (
                    sourceHeight *
                            scale
                    )
                .roundToInt()
                .coerceAtLeast(1)

        if (
            targetWidth ==
            sourceWidth &&
            targetHeight ==
            sourceHeight
        ) {
            return bitmap
        }

        return Bitmap
            .createScaledBitmap(
                bitmap,
                targetWidth,
                targetHeight,
                true,
            )
    }

    private fun resolveVideoFrameTimeUs(
        item: VideoItem,
    ): Long {
        val durationMs =
            item.durationMs
                ?.takeIf {
                    it > 0L
                }
                ?: return 0L

        val preferredMs =
            durationMs /
                    10L

        return preferredMs
            .coerceAtMost(
                5_000L,
            ) *
                1_000L
    }

    private fun resolveVideoFrameSize(
        item: VideoItem,
        targetPixelSize: Int,
    ): Pair<Int, Int> {
        val sourceWidth =
            item.width
                ?.takeIf {
                    it > 0
                }

        val sourceHeight =
            item.height
                ?.takeIf {
                    it > 0
                }

        if (
            sourceWidth == null ||
            sourceHeight == null
        ) {
            return targetPixelSize to
                    targetPixelSize
        }

        val shortSide =
            min(
                sourceWidth,
                sourceHeight,
            )
                .toFloat()

        val scale =
            min(
                1f,
                targetPixelSize
                    .toFloat() /
                        shortSide,
            )

        val targetWidth =
            (
                    sourceWidth *
                            scale
                    )
                .roundToInt()
                .coerceAtLeast(1)

        val targetHeight =
            (
                    sourceHeight *
                            scale
                    )
                .roundToInt()
                .coerceAtLeast(1)

        return targetWidth to
                targetHeight
    }

    private fun normalizeTargetSize(
        targetPixelSize: Int,
        highQualityAudioArtwork: Boolean,
    ): Int {
        val maximumTarget =
            if (highQualityAudioArtwork) {
                MaxHighQualityAudioArtworkPixelSize
            } else {
                MaxTargetPixelSize
            }

        return targetPixelSize.coerceIn(
            MinTargetPixelSize,
            maximumTarget,
        )
    }

    private fun cacheKey(
        item: MediaItem,
        targetPixelSize: Int,
        highQualityAudioArtwork: Boolean,
    ): String {
        return buildString {
            append(
                failureKey(item),
            )
            append(':')
            append(
                if (highQualityAudioArtwork) {
                    "hq"
                } else {
                    "normal"
                },
            )
            append(':')
            append(
                targetPixelSize,
            )
        }
    }

    private fun failureKey(
        item: MediaItem,
    ): String {
        return buildString {
            append(
                item.mediaType.name,
            )
            append(':')
            append(
                item.uri,
            )
            append(':')
            append(
                item.dateModifiedEpochSeconds
                    ?: 0L,
            )
        }
    }

    private fun isFailed(
        key: String,
    ): Boolean {
        return synchronized(
            failedKeys,
        ) {
            failedKeys.contains(
                key,
            )
        }
    }

    private fun rememberFailure(
        key: String,
    ) {
        synchronized(
            failedKeys,
        ) {
            if (
                failedKeys.size >=
                MaxFailedKeys
            ) {
                val iterator =
                    failedKeys.iterator()

                if (
                    iterator.hasNext()
                ) {
                    iterator.next()
                    iterator.remove()
                }
            }

            failedKeys.add(
                key,
            )
        }
    }
}