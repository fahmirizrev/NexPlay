package com.nexplay.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayHubScreenTest {
    @Test
    fun clampPosition_keepsPositionInsideDuration() {
        assertEquals(
            0L,
            clampPlayHubPosition(
                positionMs = -1_000L,
                durationMs = 60_000L,
            ),
        )

        assertEquals(
            30_000L,
            clampPlayHubPosition(
                positionMs = 30_000L,
                durationMs = 60_000L,
            ),
        )

        assertEquals(
            60_000L,
            clampPlayHubPosition(
                positionMs = 90_000L,
                durationMs = 60_000L,
            ),
        )
    }

    @Test
    fun clampPosition_withoutDuration_keepsNonNegativeRuntimePosition() {
        assertEquals(
            12_000L,
            clampPlayHubPosition(
                positionMs = 12_000L,
                durationMs = 0L,
            ),
        )

        assertEquals(
            0L,
            clampPlayHubPosition(
                positionMs = -500L,
                durationMs = 0L,
            ),
        )
    }

    @Test
    fun progressRatio_isClampedAndSafeWithoutDuration() {
        assertEquals(
            0f,
            playHubProgressRatio(
                positionMs = 30_000L,
                durationMs = 0L,
            ),
        )

        assertEquals(
            0.5f,
            playHubProgressRatio(
                positionMs = 30_000L,
                durationMs = 60_000L,
            ),
        )

        assertEquals(
            1f,
            playHubProgressRatio(
                positionMs = 90_000L,
                durationMs = 60_000L,
            ),
        )
    }
}
