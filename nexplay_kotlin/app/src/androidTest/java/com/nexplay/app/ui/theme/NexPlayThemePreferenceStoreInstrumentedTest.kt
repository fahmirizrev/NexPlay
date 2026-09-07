package com.nexplay.app.ui.theme

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexplay.app.data.preferences.NexPlayPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NexPlayThemePreferenceStoreInstrumentedTest {
    @Test
    fun applicationPreferences_persistAcrossRepositoryInstances() =
        runBlocking {
            val context =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext

            val repository =
                NexPlayPreferencesRepository(
                    context,
                )

            repository.setThemeMode(
                NexPlayThemeMode.DARK.name,
            )

            repository.setFolderSort(
                fieldName =
                    "MEDIA_COUNT",
                directionName =
                    "DESCENDING",
            )

            repository.setMediaSort(
                fieldName =
                    "DURATION",
                directionName =
                    "ASCENDING",
            )

            val persisted =
                NexPlayPreferencesRepository(
                    context,
                )
                    .preferences
                    .first()

            assertEquals(
                NexPlayThemeMode.DARK.name,
                persisted.themeModeName,
            )

            assertEquals(
                "MEDIA_COUNT",
                persisted.folderSortFieldName,
            )

            assertEquals(
                "DESCENDING",
                persisted.folderSortDirectionName,
            )

            assertEquals(
                "DURATION",
                persisted.mediaSortFieldName,
            )

            assertEquals(
                "ASCENDING",
                persisted.mediaSortDirectionName,
            )

            repository.setThemeMode(
                NexPlayThemeMode.SYSTEM.name,
            )

            repository.setFolderSort(
                fieldName =
                    "NAME",
                directionName =
                    "ASCENDING",
            )

            repository.setMediaSort(
                fieldName =
                    "NAME",
                directionName =
                    "ASCENDING",
            )
        }
}
