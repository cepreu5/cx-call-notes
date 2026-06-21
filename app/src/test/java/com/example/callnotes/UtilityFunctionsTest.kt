package com.example.callnotes

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilityFunctionsTest {

    @Test
    fun `parseColor returns default for default string`() {
        val default = Color.Blue
        val result = parseColor("default", default)
        assertEquals(default, result)
    }

    @Test
    fun `parseColor returns default for invalid hex`() {
        val default = Color.Blue
        val result = parseColor("invalid", default)
        assertEquals(default, result)
    }
}
