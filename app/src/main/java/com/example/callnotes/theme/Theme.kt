package com.example.callnotes.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ColorConstants.Primary,
    onPrimary = ColorConstants.OnPrimary,
    secondary = ColorConstants.Secondary,
    onSecondary = ColorConstants.OnSecondary,
    tertiary = ColorConstants.Tertiary,
    onTertiary = ColorConstants.OnTertiary,
    background = ColorConstants.Background,
    onBackground = ColorConstants.OnBackground,
    surface = ColorConstants.Surface,
    onSurface = ColorConstants.OnSurface,
    surfaceContainerLow = ColorConstants.SurfaceContainerLow,
    primaryContainer = ColorConstants.PrimaryContainer,
    secondaryContainer = ColorConstants.SecondaryContainer,
    tertiaryContainer = ColorConstants.TertiaryContainer,
)

@Composable
fun CallNotesTheme(
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("cx_call_notes_prefs", android.content.Context.MODE_PRIVATE) }
  val primaryHex = prefs.getString("theme_primary", "default") ?: "default"
  val secondaryHex = prefs.getString("theme_secondary", "default") ?: "default"
  val tertiaryHex = prefs.getString("theme_tertiary", "default") ?: "default"
  val parsedPrimary = try {
    if (primaryHex == "default") LightColorScheme.primary else Color(android.graphics.Color.parseColor(primaryHex))
  } catch (_: Exception) {
    LightColorScheme.primary
  }
  val parsedSecondary = try {
    if (secondaryHex == "default") LightColorScheme.secondary else Color(android.graphics.Color.parseColor(secondaryHex))
  } catch (_: Exception) {
    LightColorScheme.secondary
  }
  val parsedTertiary = try {
    if (tertiaryHex == "default") LightColorScheme.tertiary else Color(android.graphics.Color.parseColor(tertiaryHex))
  } catch (_: Exception) {
    LightColorScheme.tertiary
  }
  val dynamicScheme = LightColorScheme.copy(
    primary = parsedPrimary,
    secondary = parsedSecondary,
    tertiary = parsedTertiary
  )
  MaterialTheme(colorScheme = dynamicScheme, typography = Typography, content = content)
}
