package com.example.callnotes.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorConstantsTest {

    @Test
    fun `contrastOn returns dark color for light background`() {
        val lightColor = Color(0xFFFFFFFF)
        assertEquals(Color(0xFF2A2A28), ColorConstants.contrastOn(lightColor))
    }

    @Test
    fun `contrastOn returns dark color for medium light background`() {
        val color = Color(0xFFAAAAAA)
        assertEquals(Color(0xFF2A2A28), ColorConstants.contrastOn(color))
    }

    @Test
    fun `contrastOn returns light color for dark background`() {
        val darkColor = Color(0xFF000000)
        assertEquals(Color.White, ColorConstants.contrastOn(darkColor))
    }

    @Test
    fun `contrastOn returns light color for medium dark background`() {
        val color = Color(0xFF333333)
        assertEquals(Color.White, ColorConstants.contrastOn(color))
    }

    @Test
    fun `contrastOn boundary at luminance 0_5`() {
        val gray = Color(0xFF808080)
        val result = ColorConstants.contrastOn(gray)
        assertEquals(Color(0xFF2A2A28), result)
    }
}
