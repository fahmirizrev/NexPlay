package com.nexplay.app.playback

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexplay.app.data.media.AudioMediaScanResult
import com.nexplay.app.data.media.AudioMediaScanner
import com.nexplay.app.data.media.VideoMediaScanResult
import com.nexplay.app.data.media.VideoMediaScanner
import com.nexplay.app.domain.model.AudioItem
import com.nexplay.app.domain.model.VideoItem
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackEngineInstrumentedTest {
    @Test
    fun audioPlayback_supportsPreparePlayPauseAndSeek() {
        val audio =
            firstReadableAudio()

        val engine =
            createEngine()

        try {
            runOnMain {
                engine.prepare(
                    audio,
                )
            }

            waitUntil(
                message =
                    "Audio did not become ready",
            ) {
                engine.state.value.status ==
                        PlaybackStatus.READY ||
                        engine.state.value.status ==
                        PlaybackStatus.ERROR
            }

            assertEquals(
                PlaybackStatus.READY,
                engine.state.value.status,
            )
            assertNull(
                engine.state.value.error,
            )

            runOnMain {
                engine.play()
            }

            waitUntil(
                message =
                    "Audio did not start playing",
            ) {
                engine.state.value.isPlaying ||
                        engine.state.value.status ==
                        PlaybackStatus.ENDED ||
                        engine.state.value.status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                engine.state.value.error,
            )
            assertTrue(
                engine.state.value.isPlaying ||
                        engine.state.value.status ==
                        PlaybackStatus.ENDED,
            )

            runOnMain {
                engine.pause()
            }

            waitUntil(
                message =
                    "Audio did not pause",
            ) {
                !engine.state.value.isPlaying
            }

            val duration =
                engine.state.value.durationMs
                    ?: audio.durationMs

            val seekTarget =
                when {
                    duration == null ||
                            duration <= 0L ->
                        0L

                    duration > 2_000L ->
                        1_000L

                    else ->
                        duration / 2L
                }

            if (seekTarget > 0L) {
                runOnMain {
                    engine.seek(
                        seekTarget,
                    )
                }

                waitUntil(
                    message =
                        "Audio seek position was not published",
                ) {
                    abs(
                        engine.state.value.positionMs -
                                seekTarget,
                    ) <= 500L
                }
            }
        } finally {
            runOnMain {
                engine.release()
            }
        }
    }

    @Test
    fun videoPlayback_supportsPreparePlayAndPause() {
        val video =
            firstReadableVideo()

        val engine =
            createEngine()

        try {
            runOnMain {
                engine.prepare(
                    video,
                )
            }

            waitUntil(
                message =
                    "Video did not become ready",
            ) {
                engine.state.value.status ==
                        PlaybackStatus.READY ||
                        engine.state.value.status ==
                        PlaybackStatus.ERROR
            }

            assertEquals(
                PlaybackStatus.READY,
                engine.state.value.status,
            )
            assertNull(
                engine.state.value.error,
            )

            runOnMain {
                engine.play()
            }

            waitUntil(
                message =
                    "Video did not start playing",
            ) {
                engine.state.value.isPlaying ||
                        engine.state.value.status ==
                        PlaybackStatus.ENDED ||
                        engine.state.value.status ==
                        PlaybackStatus.ERROR
            }

            assertNull(
                engine.state.value.error,
            )
            assertTrue(
                engine.state.value.isPlaying ||
                        engine.state.value.status ==
                        PlaybackStatus.ENDED,
            )

            runOnMain {
                engine.pause()
            }

            waitUntil(
                message =
                    "Video did not pause",
            ) {
                !engine.state.value.isPlaying
            }
        } finally {
            runOnMain {
                engine.release()
            }
        }
    }

    @Test
    fun invalidSource_becomesErrorStateWithoutCrash() {
        val engine =
            createEngine()

        val invalidItem =
            AudioItem(
                id = "invalid",
                uri = " ",
                displayName =
                    "invalid.mp3",
                title = "Invalid",
                durationMs = null,
                mimeType = "audio/mpeg",
                folder = null,
                sizeBytes = null,
                dateAddedEpochSeconds = null,
                dateModifiedEpochSeconds = null,
                artist = null,
                album = null,
                albumId = null,
                track = null,
            )

        try {
            runOnMain {
                engine.prepare(
                    invalidItem,
                )
            }

            assertEquals(
                PlaybackStatus.ERROR,
                engine.state.value.status,
            )
            assertEquals(
                PlaybackErrorKind.SOURCE,
                engine.state.value.error?.kind,
            )
        } finally {
            runOnMain {
                engine.release()
            }
        }
    }

    private fun firstReadableAudio(): AudioItem {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val result =
            AudioMediaScanner(
                context,
            ).scan()

        assertTrue(
            "Expected audio MediaStore scan to succeed, but got $result",
            result is
                    AudioMediaScanResult.Success,
        )

        val items =
            (result as
                    AudioMediaScanResult.Success)
                .items

        assertTrue(
            "Verification device must contain at least one readable audio item",
            items.isNotEmpty(),
        )

        return items.first()
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

    private fun createEngine(): PlaybackEngine {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        lateinit var engine:
                PlaybackEngine

        runOnMain {
            engine =
                PlaybackEngine(
                    context,
                )
        }

        return engine
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