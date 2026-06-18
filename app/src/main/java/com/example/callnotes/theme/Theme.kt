package com.example.callnotes.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
/*
private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceContainerLow = Color(0xFFF3EDF7),
    secondaryContainer = Color(0xFFE8DEF8),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
  )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF006A6A),          // наситено тийл
        secondary = Color(0xFF4A635F),        // тъмна мента
        tertiary = Color(0xFF38668C),         // модерен син акцент
        background = Color(0xFFF7FAF9),       // много светъл, почти бял фон
        surface = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFE8F1EF),
        secondaryContainer = Color(0xFFBEEDEA), // пастелна мента
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1A1C1C),
        onSurface = Color(0xFF1A1C1C),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFFDD6B20),          // ярко оранжево (основен акцент)
        onPrimary = Color.White,

        secondary = Color(0xFFF6AD55),        // по-светъл оранжев нюанс
        onSecondary = Color(0xFF3A2F1A),

        tertiary = Color(0xFF4A5568),         // синьо-сив контраст (много модерен)
        onTertiary = Color.White,

        background = Color(0xFFFDF7F2),       // топъл почти-бял фон
        onBackground = Color(0xFF1F1A17),

        surface = Color(0xFFFFFCFA),
        onSurface = Color(0xFF1F1A17),

        surfaceContainerLow = Color(0xFFF3EDE7), // леко повдигнат слой
        secondaryContainer = Color(0xFFFFE0C2),  // пастелно оранжево за контейнери
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF4FD8D8),          // ярък тийл за тъмна тема
        secondary = Color(0xFFB1CCC7),        // светла мента
        tertiary = Color(0xFF9CCBFF),         // светъл син акцент
        background = Color(0xFF111414),       // почти черен фон
        surface = Color(0xFF1A1C1C),
        surfaceContainerLow = Color(0xFF1F2424),
        secondaryContainer = Color(0xFF1F4E4E), // тъмен тийл контейнер
        onPrimary = Color(0xFF003737),
        onSecondary = Color(0xFF1A1C1C),
        onTertiary = Color(0xFF003355),
        onBackground = Color(0xFFE1E3E3),
        onSurface = Color(0xFFE1E3E3),
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFF8C42),          // ярко оранжево за тъмна тема
        onPrimary = Color(0xFF3A1F0A),

        secondary = Color(0xFFFFB878),        // светъл топъл оранжев нюанс
        onSecondary = Color(0xFF3A2A1A),

        tertiary = Color(0xFFCBD5E0),         // светъл синьо-сив за контраст
        onTertiary = Color(0xFF1A202C),

        background = Color(0xFF1E1A17),       // тъмен топъл фон (не черен)
        onBackground = Color(0xFFEDE7E3),

        surface = Color(0xFF26221F),
        onSurface = Color(0xFFEDE7E3),

        surfaceContainerLow = Color(0xFF2E2926), // леко повдигнат слой
        secondaryContainer = Color(0xFF5A3A1F),  // тъмен оранжев контейнер
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB3D6),          // светло пастелно розово
        onPrimary = Color(0xFF3A1F28),

        secondary = Color(0xFFCCE9FF),        // светло пастелно синьо
        onSecondary = Color(0xFF1A2630),

        tertiary = Color(0xFFD4F8D2),         // светла мента
        onTertiary = Color(0xFF1A2E1A),

        background = Color(0xFF2A242A),       // тъмен лилаво‑сив фон (не черен)
        onBackground = Color(0xFFF3EDF2),

        surface = Color(0xFF332D33),          // тъмен, но мек surface
        onSurface = Color(0xFFF3EDF2),

        surfaceContainerLow = Color(0xFF3C353C), // леко повдигнат слой
        secondaryContainer = Color(0xFF2F3E4A),  // тъмен син контейнер
    )
*/

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFFF7A8B8),          // розово бонбон
        onPrimary = Color(0xFF3A1F25),

        secondary = Color(0xFF6ED3CF),        // тюркоаз
        onSecondary = Color(0xFF123233),

        tertiary = Color(0xFFC39BD3),         // лилаво
        onTertiary = Color(0xFF2E1F33),

        background = Color(0xFFF5F5F2),       // пастелен фон
        onBackground = Color(0xFF2A2A28),

        surface = Color(0xFFFFFFFF),          // бял surface
        onSurface = Color(0xFF2A2A28),

        surfaceContainerLow = Color(0xFFECECE8), // леко сивкав слой

        // Допълнителни пастелни панели
        primaryContainer = Color(0xFFF9E79F), // жълто
        secondaryContainer = Color(0xFFABE188), // лайм зелено
        tertiaryContainer = Color(0xFFF5B67A), // оранжево
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

