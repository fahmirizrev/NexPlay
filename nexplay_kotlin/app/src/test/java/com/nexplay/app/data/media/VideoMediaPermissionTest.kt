package com.nexplay.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMediaPermissionTest {
    @Test
    fun android12LAndLower_requiresReadExternalStorage() {
        assertEquals(
            "android.permission.READ_EXTERNAL_STORAGE",
            VideoMediaPermission.requiredPermission(32),
        )
    }

    @Test
    fun android13AndHigher_requiresReadMediaVideo() {
        assertEquals(
            "android.permission.READ_MEDIA_VIDEO",
            VideoMediaPermission.requiredPermission(33),
        )

        assertEquals(
            "android.permission.READ_MEDIA_VIDEO",
            VideoMediaPermission.requiredPermission(37),
        )
    }
}