package com.example.callnotes.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
*/
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

@Composable
fun CallNotesTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}

