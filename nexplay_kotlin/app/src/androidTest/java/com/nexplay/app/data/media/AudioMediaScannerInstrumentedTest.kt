package com.nexplay.app.data.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioMediaScannerInstrumentedTest {
    @Test
    fun scanner_readsAudioFromDeviceMediaStore() {
        val context =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val result = AudioMediaScanner(context).scan()

        assertTrue(
            "Expected MediaStore scan to succeed, but got $result",
            result is AudioMediaScanResult.Success,
        )

        val items =
            (result as AudioMediaScanResult.Success).items

        assertFalse(
            "Verification device must contain at least one readable audio item",
            items.isEmpty(),
        )

        items.forEach { item ->
            assertTrue(item.id.isNotBlank())
            assertTrue(item.uri.startsWith("content://"))
            assertTrue(item.displayName.isNotBlank())
            assertTrue(item.title.isNotBlank())
        }
    }
}