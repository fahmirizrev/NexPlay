package com.nexplay.app.playback

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexplay.app.data.media.VideoMediaScanResult
import com.nexplay.app.data.media.VideoMediaScanner
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.MediaItem
import com.nexplay.app.domain.model.VideoItem
import com.nexplay.app.domain.queue.PlayHubContext
import com.nexplay.app.domain.queue.RepeatMode
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueuePlaybackCoordinatorInstrumentedTest {
    @Test
    fun mixedQueueNavigation_keepsQueueAndPlaybackSynchronized() {
        val audio =
            createSilentAudio(
                id = "mixed-audio",
                durationMs = 5_000L,
            )

        val video =
            firstReadableVideo()

        val coordinator =
            createCoordinator()

        try {
            runOnMain {
                coordinator.setQueue(
                    items =
                        listOf(
                            audio,
                            video,
                        ),
                    currentIndex = 0,
                    playbackContext =
                        PlayHubContext(
                            section =
                                "Instrumentation",
                        ),
                )
            }

            waitUntil(
                message =
                    "Audio queue selection did not reach playback",
            ) {
                coordinator
                    .playbackState
                    .value
                    .currentMedia == audio ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                audio,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )
            assertEquals(
                audio,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )

            runOnMain {
                coordinator.next()
            }

            waitUntil(
                message =
                    "Next did not switch playback to video",
            ) {
                coordinator
                    .playbackState
                    .value
                    .currentMedia == video ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                video,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )
            assertEquals(
                video,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )

            runOnMain {
                coordinator.previous()
            }

            waitUntil(
                message =
                    "Previous did not restore audio playback",
            ) {
                coordinator
                    .playbackState
                    .value
                    .currentMedia == audio ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                audio,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )
            assertEquals(
                audio,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
        } finally {
            runOnMain {
                coordinator.release()
            }

            deleteTestMedia(
                audio,
            )
        }
    }

    @Test
    fun playbackEnd_advancesQueueAndStartsNextItem() {
        val first =
            createSilentAudio(
                id = "completion-first",
                durationMs = 300L,
            )

        val second =
            createSilentAudio(
                id = "completion-second",
                durationMs = 5_000L,
            )

        val coordinator =
            createCoordinator()

        try {
            runOnMain {
                coordinator.setQueue(
                    items =
                        listOf(
                            first,
                            second,
                        ),
                    currentIndex = 0,
                    playbackContext =
                        PlayHubContext(
                            section =
                                "Instrumentation",
                        ),
                )
            }

            waitUntil(
                message =
                    "End-of-track did not advance queue",
            ) {
                coordinator
                    .queueState
                    .value
                    .currentItem == second ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                1,
                coordinator
                    .queueState
                    .value
                    .currentIndex,
            )
            assertEquals(
                second,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )

            waitUntil(
                message =
                    "Advanced queue item did not reach playback",
            ) {
                coordinator
                    .playbackState
                    .value
                    .currentMedia == second ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                second,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
        } finally {
            runOnMain {
                coordinator.release()
            }

            deleteTestMedia(
                first,
                second,
            )
        }
    }

    @Test
    fun repeatOne_replaysCurrentItemAfterCompletion() {
        val audio =
            createSilentAudio(
                id = "repeat-one",
                durationMs = 250L,
            )

        val coordinator =
            createCoordinator()

        try {
            runOnMain {
                coordinator.setRepeat(
                    RepeatMode.ONE,
                )

                coordinator.setQueue(
                    items = listOf(audio),
                    currentIndex = 0,
                    playbackContext =
                        PlayHubContext(
                            section =
                                "Instrumentation",
                        ),
                )
            }

            waitUntil(
                message =
                    "Repeat-one media did not start",
            ) {
                coordinator
                    .playbackState
                    .value
                    .isPlaying ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )

            Thread.sleep(
                700L,
            )

            waitUntil(
                message =
                    "Repeat ONE remained ended instead of replaying",
            ) {
                coordinator
                    .playbackState
                    .value
                    .isPlaying ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                0,
                coordinator
                    .queueState
                    .value
                    .currentIndex,
            )
            assertEquals(
                audio,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )
            assertEquals(
                audio,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
        } finally {
            runOnMain {
                coordinator.release()
            }

            deleteTestMedia(
                audio,
            )
        }
    }

    @Test
    fun stopPlayback_retainsQueueAndPlayRestartsCurrentItem() {
        val first =
            createSilentAudio(
                id = "retained-first",
                durationMs = 5_000L,
            )

        val second =
            createSilentAudio(
                id = "retained-second",
                durationMs = 5_000L,
            )

        val context =
            PlayHubContext(
                section =
                    "Retained Queue",
            )

        val coordinator =
            createCoordinator()

        try {
            runOnMain {
                coordinator.setQueue(
                    items =
                        listOf(
                            first,
                            second,
                        ),
                    currentIndex = 0,
                    playbackContext =
                        context,
                )
            }

            waitUntil(
                message =
                    "Retained-queue test media did not start",
            ) {
                coordinator
                    .playbackState
                    .value
                    .isPlaying ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )

            runOnMain {
                coordinator.stopPlayback()
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
            assertEquals(
                PlaybackStatus.IDLE,
                coordinator
                    .playbackState
                    .value
                    .status,
            )
            assertEquals(
                listOf(
                    first,
                    second,
                ),
                coordinator
                    .queueState
                    .value
                    .items,
            )
            assertEquals(
                0,
                coordinator
                    .queueState
                    .value
                    .currentIndex,
            )
            assertEquals(
                first,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )
            assertEquals(
                context,
                coordinator
                    .queueState
                    .value
                    .playbackContext,
            )

            runOnMain {
                coordinator.play()
            }

            waitUntil(
                message =
                    "Retained queue did not restart current item",
            ) {
                coordinator
                    .playbackState
                    .value
                    .currentMedia == first &&
                        (
                            coordinator
                                .playbackState
                                .value
                                .isPlaying ||
                                coordinator
                                    .playbackState
                                    .value
                                    .status ==
                                PlaybackStatus.ERROR
                        )
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                first,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
            assertTrue(
                "Retained queue restart did not begin near 0:00",
                coordinator
                    .playbackState
                    .value
                    .positionMs <
                    1_000L,
            )
            assertEquals(
                listOf(
                    first,
                    second,
                ),
                coordinator
                    .queueState
                    .value
                    .items,
            )
            assertEquals(
                context,
                coordinator
                    .queueState
                    .value
                    .playbackContext,
            )
        } finally {
            runOnMain {
                coordinator.release()
            }

            deleteTestMedia(
                first,
                second,
            )
        }
    }

    @Test
    fun abRepeat_loopsWithinMarkersAndResetsOnMediaChange() {
        val first =
            createSilentAudio(
                id = "ab-repeat-first",
                durationMs = 3_000L,
            )

        val second =
            createSilentAudio(
                id = "ab-repeat-second",
                durationMs = 3_000L,
            )

        val coordinator =
            createCoordinator()

        try {
            runOnMain {
                coordinator.setQueue(
                    items =
                        listOf(
                            first,
                            second,
                        ),
                    currentIndex = 0,
                    playbackContext =
                        PlayHubContext(
                            section =
                                "Instrumentation",
                        ),
                )
            }

            waitUntil(
                message =
                    "A-B test media did not start",
            ) {
                coordinator
                    .playbackState
                    .value
                    .isPlaying ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )

            runOnMain {
                coordinator.seek(
                    250L,
                )
                coordinator.toggleAbRepeatMarker()
            }

            val start =
                coordinator
                    .playbackState
                    .value
                    .abRepeatStartMs

            assertTrue(
                "A marker was not set",
                start != null,
            )
            assertNull(
                coordinator
                    .playbackState
                    .value
                    .abRepeatEndMs,
            )

            runOnMain {
                coordinator.seek(
                    650L,
                )
                coordinator.toggleAbRepeatMarker()
            }

            val end =
                coordinator
                    .playbackState
                    .value
                    .abRepeatEndMs

            assertTrue(
                "B marker was not set after A",
                end != null &&
                        start != null &&
                        end > start,
            )
            assertTrue(
                coordinator
                    .playbackState
                    .value
                    .isAbRepeatEnabled,
            )

            Thread.sleep(
                1_200L,
            )

            assertEquals(
                first,
                coordinator
                    .queueState
                    .value
                    .currentItem,
            )
            assertEquals(
                first,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
            assertTrue(
                "A-B repeat did not loop near the selected range",
                coordinator
                    .playbackState
                    .value
                    .positionMs <
                        1_200L,
            )

            runOnMain {
                coordinator.toggleAbRepeatMarker()
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .abRepeatStartMs,
            )
            assertNull(
                coordinator
                    .playbackState
                    .value
                    .abRepeatEndMs,
            )

            runOnMain {
                coordinator.seek(
                    200L,
                )
                coordinator.toggleAbRepeatMarker()
                coordinator.next()
            }

            waitUntil(
                message =
                    "Next media did not replace A-B test media",
            ) {
                coordinator
                    .playbackState
                    .value
                    .currentMedia == second ||
                        coordinator
                            .playbackState
                            .value
                            .status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                coordinator
                    .playbackState
                    .value
                    .error,
            )
            assertEquals(
                second,
                coordinator
                    .playbackState
                    .value
                    .currentMedia,
            )
            assertNull(
                coordinator
                    .playbackState
                    .value
                    .abRepeatStartMs,
            )
            assertNull(
                coordinator
                    .playbackState
                    .value
                    .abRepeatEndMs,
            )
        } finally {
            runOnMain {
                coordinator.release()
            }

            deleteTestMedia(
                first,
                second,
            )
        }
    }

    private fun firstReadableVideo(): VideoItem {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val result =
            VideoMediaScanner(
                context,
            ).scan()

        assertTrue(
            "Expected video MediaStore scan to succeed, but got $result",
            result is
                    VideoMediaScanResult.Success,
        )

        val items =
            (result as
                    VideoMediaScanResult.Success)
                .items

        assertTrue(
            "Verification device must contain at least one readable video item",
            items.isNotEmpty(),
        )

        return items.first()
    }

    private fun createCoordinator():
            QueuePlaybackCoordinator {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        lateinit var coordinator:
                QueuePlaybackCoordinator

        runOnMain {
            coordinator =
                QueuePlaybackCoordinator(
                    context,
                )
        }

        return coordinator
    }

    private fun createSilentAudio(
        id: String,
        durationMs: Long,
    ): AudioItem {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val sampleRate = 8_000
        val channelCount = 1
        val bitsPerSample = 16
        val bytesPerSample =
            bitsPerSample / 8
        val blockAlign =
            channelCount *
                    bytesPerSample
        val byteRate =
            sampleRate *
                    blockAlign
        val sampleCount =
            (
                    sampleRate.toLong() *
                            durationMs /
                            1_000L
                    )
                .coerceAtLeast(1L)
                .toInt()
        val dataSize =
            sampleCount *
                    blockAlign

        val file =
            File(
                context.cacheDir,
                "queue_playback_${id}_${System.nanoTime()}.wav",
            )

        val buffer =
            ByteBuffer
                .allocate(
                    44 + dataSize,
                )
                .order(
                    ByteOrder.LITTLE_ENDIAN,
                )

        buffer.put(
            "RIFF".toByteArray(
                Charsets.US_ASCII,
            ),
        )
        buffer.putInt(
            36 + dataSize,
        )
        buffer.put(
            "WAVE".toByteArray(
                Charsets.US_ASCII,
            ),
        )
        buffer.put(
            "fmt ".toByteArray(
                Charsets.US_ASCII,
            ),
        )
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(
            channelCount.toShort(),
        )
        buffer.putInt(
            sampleRate,
        )
        buffer.putInt(
            byteRate,
        )
        buffer.putShort(
            blockAlign.toShort(),
        )
        buffer.putShort(
            bitsPerSample.toShort(),
        )
        buffer.put(
            "data".toByteArray(
                Charsets.US_ASCII,
            ),
        )
        buffer.putInt(
            dataSize,
        )

        FileOutputStream(
            file,
        ).use { output ->
            output.write(
                buffer.array(),
            )
        }

        return AudioItem(
            id = id,
            uri = file.absolutePath,
            displayName = "$id.wav",
            title = id,
            durationMs = durationMs,
            mimeType = "audio/wav",
            folder = null,
            sizeBytes = file.length(),
            dateAddedEpochSeconds = null,
            dateModifiedEpochSeconds = null,
            artist = null,
            album = null,
            albumId = null,
            track = null,
        )
    }

    private fun deleteTestMedia(
        vararg items: MediaItem,
    ) {
        items.forEach { item ->
            if (
                item.uri.isNotBlank() &&
                !item.uri.contains("://")
            ) {
                File(
                    item.uri,
                ).delete()
            }
        }
    }

    private fun runOnMain(
        block: () -> Unit,
    ) {
        InstrumentationRegistry
            .getInstrumentation()
            .runOnMainSync(
                Runnable {
                    block()
                },
            )
    }

    private fun waitUntil(
        message: String,
        timeoutMs: Long = 10_000L,
        condition: () -> Boolean,
    ) {
        val deadline =
            SystemClock.elapsedRealtime() +
                    timeoutMs

        while (
            SystemClock.elapsedRealtime() <
            deadline
        ) {
            if (condition()) {
                return
            }

            Thread.sleep(
                50L,
            )
        }

        assertTrue(
            message,
            condition(),
        )
    }
}