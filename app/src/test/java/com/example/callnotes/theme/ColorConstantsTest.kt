package com.example.callnotes.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorConstantsTest {

    @Test
    fun `contrastOn returns dark for white`() {
        val result = ColorConstants.contrastOn(Color.White)
        assertTrue("Expected dark color for white background", result.red < 0.5f)
    }

    @Test
    fun `contrastOn returns dark for light pink`() {
        val lightPink = Color(0xFFF7A8B8)
        val result = ColorConstants.contrastOn(lightPink)
        assertTrue("Expected dark color for light pink", result.red < 0.5f)
    }

    @Test
    fun `contrastOn returns dark for light secondary`() {
        val lightSecondary = Color(0xFF6ED3CF)
        val result = ColorConstants.contrastOn(lightSecondary)
        assertTrue("Expected dark color for light secondary", result.red < 0.5f)
    }

    @Test
    fun `contrastOn returns dark for light tertiary`() {
        val lightTertiary = Color(0xFFC39BD3)
        val result = ColorConstants.contrastOn(lightTertiary)
        assertTrue("Expected dark color for light tertiary", result.red < 0.5f)
    }

    @Test
    fun `contrastOn returns white for black`() {
        val result = ColorConstants.contrastOn(Color.Black)
        assertEquals(1.0f, result.red, 0.01f)
        assertEquals(1.0f, result.green, 0.01f)
        assertEquals(1.0f, result.blue, 0.01f)
    }

    @Test
    fun `contrastOn returns dark for luminance above 0_5`() {
        val lightGray = Color(0xFFBBBBBB)
        val result = ColorConstants.contrastOn(lightGray)
        assertTrue("Expected dark color for light gray", result.red < 0.5f)
    }

    @Test
    fun `contrastOn returns white for luminance below 0_5`() {
        val darkGray = Color(0xFF444444)
        val result = ColorConstants.contrastOn(darkGray)
        assertEquals(1.0f, result.red, 0.01f)
    }

    @Test
    fun `contrastOn boundary at exactly 0_5 luminance`() {
        val midGray = Color(0xFF808080)
        val result = ColorConstants.contrastOn(midGray)
        assertTrue("Expected dark color at boundary", result.red < 0.5f)
    }

    @Test
    fun `contrastOn works with custom light color`() {
        val customLight = Color(0xFFE0E0E0)
        val result = ColorConstants.contrastOn(customLight)
        assertTrue("Expected dark color for custom light", result.red < 0.5f)
    }

    @Test
    fun `contrastOn works with custom dark color`() {
        val customDark = Color(0xFF1A1A2E)
        val result = ColorConstants.contrastOn(customDark)
        assertEquals(1.0f, result.red, 0.01f)
    }
}
