package com.example.callnotes.theme

import androidx.compose.ui.graphics.Color

object ColorConstants {
    //  шрифтове - основен, контакти, бележки
    val Primary = Color(0xFF4A3B30)
    val Secondary = Color(0xFF995C00)
    val Tertiary = Color(0xFF5C3317)

    val Background = Color(0xFFFFFBF5)

    // Панели
    val SurfaceContainerLow = Color(0xFFFFE8CC)
    val SecondaryContainer = Color(0xFFFFD8A8)

    //  PrimaryContainer — предава се на темата, но не се използва директно в кода ( Material3 компоненти може да я ползват вътрешно)
    val PrimaryContainer = Color(0xFFF9E79F)

    val ButtonBackground = Color(0xFF757575)
    val ButtonFontColor = Color.White

    // Етикети
    val TagChipBackground = Color(0xFFF9E79F)
    val TagChipBorder = Color(0xFFF7A8B8)
    val TagChipText = Color(0xFF2A2A28)
    //  TertiaryContainer — използва се в FormTagChip за фон на избран етикет (PostCallNoteActivity.kt:265)
    val TertiaryContainer = Color(0xFFF5B67A)

    private val DarkOn = Color(0xFF2A2A28)
    private val LightOn = Color.White

    fun contrastOn(color: Color): Color {
        val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return if (luminance > 0.5f) DarkOn else LightOn
    }
}
