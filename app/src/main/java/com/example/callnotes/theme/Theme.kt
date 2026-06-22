package com.example.callnotes.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ColorConstants.Primary,
    onPrimary = ColorConstants.contrastOn(ColorConstants.Primary),
    secondary = ColorConstants.Secondary,
    onSecondary = ColorConstants.contrastOn(ColorConstants.Secondary),
    tertiary = ColorConstants.Tertiary,
    onTertiary = ColorConstants.contrastOn(ColorConstants.Tertiary),
    background = ColorConstants.Background,
    onBackground = ColorConstants.contrastOn(ColorConstants.Background),
    surface = ColorConstants.Background,
    onSurface = ColorConstants.contrastOn(ColorConstants.Background),
    surfaceContainerLow = ColorConstants.SurfaceContainerLow,
    primaryContainer = ColorConstants.PrimaryContainer,
    secondaryContainer = ColorConstants.SecondaryContainer,
    tertiaryContainer = ColorConstants.TertiaryContainer,
)

private fun parseHex(hex: String, default: Color): Color {
    if (hex == "default") return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        default
    }
}

@Composable
fun CallNotesTheme(
    themePrimary: String = "default",
    themeSecondary: String = "default",
    themeTertiary: String = "default",
    content: @Composable () -> Unit,
) {
    val parsedPrimary = parseHex(themePrimary, LightColorScheme.primary)
    val parsedSecondary = parseHex(themeSecondary, LightColorScheme.secondary)
    val parsedTertiary = parseHex(themeTertiary, LightColorScheme.tertiary)
    val dynamicScheme = LightColorScheme.copy(
        primary = parsedPrimary,
        onPrimary = ColorConstants.contrastOn(parsedPrimary),
        secondary = parsedSecondary,
        onSecondary = ColorConstants.contrastOn(parsedSecondary),
        tertiary = parsedTertiary,
        onTertiary = ColorConstants.contrastOn(parsedTertiary)
    )
    MaterialTheme(colorScheme = dynamicScheme, typography = Typography, content = content)
}
