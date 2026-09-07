package com.nexplay.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildLockPatternTest {
    @Test
    fun validPattern_requiresAtLeastFourUniqueNodes() {
        assertTrue(
            isValidChildLockPattern(
                listOf(
                    0,
                    1,
                    4,
                    7,
                ),
            ),
        )

        assertFalse(
            isValidChildLockPattern(
                listOf(
                    0,
                    1,
                    4,
                ),
            ),
        )

        assertFalse(
            isValidChildLockPattern(
                listOf(
                    0,
                    1,
                    4,
                    1,
                ),
            ),
        )
    }

    @Test
    fun encodedPattern_roundTrips() {
        val pattern =
            listOf(
                0,
                1,
                4,
                7,
            )

        assertEquals(
            pattern,
            decodeChildLockPattern(
                encodeChildLockPattern(
                    pattern,
                ),
            ),
        )
    }

    @Test
    fun decodePattern_rejectsInvalidNodes() {
        assertNull(
            decodeChildLockPattern(
                "0,1,4,9",
            ),
        )
    }
}
