package com.nexplay.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class NexPlayPreferences(
    val themeModeName: String =
        DefaultThemeModeName,
    val folderSortFieldName: String =
        DefaultFolderSortFieldName,
    val folderSortDirectionName: String =
        DefaultSortDirectionName,
    val mediaSortFieldName: String =
        DefaultMediaSortFieldName,
    val mediaSortDirectionName: String =
        DefaultSortDirectionName,
)

private val Context.nexPlayPreferencesDataStore by
preferencesDataStore(
    name = PreferencesDataStoreName,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName =
                    LegacyThemePreferencesName,
                keysToMigrate =
                    setOf(
                        ThemeModeKeyName,
                    ),
            ),
        )
    },
)

class NexPlayPreferencesRepository(
    context: Context,
) {
    private val dataStore =
        context.applicationContext
            .nexPlayPreferencesDataStore

    val preferences: Flow<NexPlayPreferences> =
        dataStore
            .data
            .catch { exception ->
                if (exception is IOException) {
                    emit(
                        emptyPreferences(),
                    )
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                NexPlayPreferences(
                    themeModeName =
                        preferences[
                            ThemeModeKey
                        ] ?: DefaultThemeModeName,
                    folderSortFieldName =
                        preferences[
                            FolderSortFieldKey
                        ] ?: DefaultFolderSortFieldName,
                    folderSortDirectionName =
                        preferences[
                            FolderSortDirectionKey
                        ] ?: DefaultSortDirectionName,
                    mediaSortFieldName =
                        preferences[
                            MediaSortFieldKey
                        ] ?: DefaultMediaSortFieldName,
                    mediaSortDirectionName =
                        preferences[
                            MediaSortDirectionKey
                        ] ?: DefaultSortDirectionName,
                )
            }

    suspend fun setThemeMode(
        modeName: String,
    ) {
        dataStore.edit { preferences ->
            preferences[
                ThemeModeKey
            ] = modeName
        }
    }

    suspend fun setFolderSort(
        fieldName: String,
        directionName: String,
    ) {
        dataStore.edit { preferences ->
            preferences[
                FolderSortFieldKey
            ] = fieldName

            preferences[
                FolderSortDirectionKey
            ] = directionName
        }
    }

    suspend fun setMediaSort(
        fieldName: String,
        directionName: String,
    ) {
        dataStore.edit { preferences ->
            preferences[
                MediaSortFieldKey
            ] = fieldName

            preferences[
                MediaSortDirectionKey
            ] = directionName
        }
    }

    private companion object {
        val ThemeModeKey =
            stringPreferencesKey(
                ThemeModeKeyName,
            )

        val FolderSortFieldKey =
            stringPreferencesKey(
                "folder_sort_field",
            )

        val FolderSortDirectionKey =
            stringPreferencesKey(
                "folder_sort_direction",
            )

        val MediaSortFieldKey =
            stringPreferencesKey(
                "media_sort_field",
            )

        val MediaSortDirectionKey =
            stringPreferencesKey(
                "media_sort_direction",
            )
    }
}

private const val PreferencesDataStoreName =
    "nexplay_preferences"

private const val LegacyThemePreferencesName =
    "nexplay_theme_preferences"

private const val ThemeModeKeyName =
    "theme_mode"

private const val DefaultThemeModeName =
    "SYSTEM"

private const val DefaultFolderSortFieldName =
    "NAME"

private const val DefaultMediaSortFieldName =
    "TITLE"

private const val DefaultSortDirectionName =
    "ASCENDING"