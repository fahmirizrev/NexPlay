package com.nexplay.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioMediaPermissionTest {
    @Test
    fun android12LAndLower_requiresReadExternalStorage() {
        assertEquals(
            "android.permission.READ_EXTERNAL_STORAGE",
            AudioMediaPermission.requiredPermission(32),
        )
    }

    @Test
    fun android13AndHigher_requiresReadMediaAudio() {
        assertEquals(
            "android.permission.READ_MEDIA_AUDIO",
            AudioMediaPermission.requiredPermission(33),
        )

        assertEquals(
            "android.permission.READ_MEDIA_AUDIO",
            AudioMediaPermission.requiredPermission(37),
        )
    }
}