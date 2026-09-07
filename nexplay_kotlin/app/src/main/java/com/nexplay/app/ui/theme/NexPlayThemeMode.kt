package com.nexplay.app.ui.theme

import com.nexplay.app.data.preferences.NexPlayPreferences

enum class NexPlayThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

internal fun NexPlayPreferences.resolveThemeMode():
        NexPlayThemeMode {
    return NexPlayThemeMode.entries
        .firstOrNull { mode ->
            mode.name ==
                themeModeName
        }
        ?: NexPlayThemeMode.SYSTEM
}
