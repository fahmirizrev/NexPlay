package com.nexplay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NexPlayShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

private val DarkColorScheme = darkColorScheme(
    primary = NexPlayDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = NexPlayDarkAccent,
    onPrimaryContainer = NexPlayDarkPrimary,
    background = NexPlayDarkBackground,
    onBackground = NexPlayDarkForeground,
    surface = NexPlayDarkSurface,
    onSurface = NexPlayDarkSurfaceForeground,
    surfaceVariant = NexPlayDarkMuted,
    onSurfaceVariant = NexPlayDarkMutedForeground,
    secondary = NexPlayDarkPrimary,
    onSecondary = Color.White,
    secondaryContainer = NexPlayDarkMuted,
    onSecondaryContainer = NexPlayDarkForeground,
    outline = NexPlayDarkOutline,
    outlineVariant = NexPlayDarkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = NexPlayLightPrimary,
    onPrimary = Color.White,
    primaryContainer = NexPlayLightAccent,
    onPrimaryContainer = NexPlayLightPrimary,
    background = NexPlayLightBackground,
    onBackground = NexPlayLightForeground,
    surface = NexPlayLightSurface,
    onSurface = NexPlayLightSurfaceForeground,
    surfaceVariant = NexPlayLightMuted,
    onSurfaceVariant = NexPlayLightMutedForeground,
    secondary = NexPlayLightPrimary,
    onSecondary = Color.White,
    secondaryContainer = NexPlayLightMuted,
    onSecondaryContainer = NexPlayLightForeground,
    outline = NexPlayLightOutline,
    outlineVariant = NexPlayLightOutline,
)

@Composable
fun NexPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme =
            if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            },
        typography = Typography,
        shapes = NexPlayShapes,
        content = content,
    )
}