package com.example.callnotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberNormalizerTest {

    @Test
    fun `normalize Bulgarian mobile number with 0 prefix`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("0888123456"))
    }

    @Test
    fun `normalize Bulgarian mobile number with 359 prefix`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("359888123456"))
    }

    @Test
    fun `normalize Bulgarian mobile number with + prefix`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("+359888123456"))
    }

    @Test
    fun `normalize number with spaces and dashes`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("0888 123 456"))
    }

    @Test
    fun `normalize number with parentheses`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("(0888) 123-456"))
    }

    @Test
    fun `normalize already normalized number`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("+359888123456"))
    }

    @Test
    fun `normalize short number unchanged`() {
        assertEquals("1234", PhoneNumberNormalizer.normalize("1234"))
    }

    @Test
    fun `normalize number with leading zeros kept for non-10-digit`() {
        assertEquals("00123", PhoneNumberNormalizer.normalize("00123"))
    }
}
