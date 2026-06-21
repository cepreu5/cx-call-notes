package com.example.callnotes

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityFunctionsTest {

    @Test
    fun `parseColor returns default for default string`() {
        val default = Color.Blue
        val result = parseColor("default", default)
        assertTrue(result == default)
    }

    @Test
    fun `parseColor returns default for empty string`() {
        val default = Color.Red
        val result = parseColor("", default)
        assertTrue(result == default)
    }

    @Test
    fun `parseColor returns default for random text`() {
        val default = Color.Green
        val result = parseColor("not-a-color", default)
        assertTrue(result == default)
    }

    @Test
    fun `parseColor returns default for partial hex`() {
        val default = Color.Gray
        val result = parseColor("#FF", default)
        assertTrue(result == default)
    }

    @Test
    fun `parseColor preserves default reference`() {
        val default = Color(0xFF123456)
        val result = parseColor("invalid", default)
        assertTrue(result == default)
    }
}
