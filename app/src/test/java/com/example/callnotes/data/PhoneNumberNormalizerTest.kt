package com.example.callnotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberNormalizerTest {

    @Test
    fun `normalize Bulgarian mobile with 0 prefix`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("0888123456"))
    }

    @Test
    fun `normalize Bulgarian mobile with 359 prefix`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("359888123456"))
    }

    @Test
    fun `normalize Bulgarian mobile with plus prefix`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("+359888123456"))
    }

    @Test
    fun `normalize with spaces`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("0888 123 456"))
    }

    @Test
    fun `normalize with dashes`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("0888-123-456"))
    }

    @Test
    fun `normalize with parentheses`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("(0888) 123-456"))
    }

    @Test
    fun `normalize with dots`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("0888.123.456"))
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
    fun `normalize 3-digit service number unchanged`() {
        assertEquals("112", PhoneNumberNormalizer.normalize("112"))
    }

    @Test
    fun `normalize landline stays as is`() {
        assertEquals("029123456", PhoneNumberNormalizer.normalize("02 912 3456"))
    }

    @Test
    fun `normalize mixed format`() {
        assertEquals("+359888123456", PhoneNumberNormalizer.normalize("+359 888 123 456"))
    }

    @Test
    fun `normalize empty string`() {
        assertEquals("", PhoneNumberNormalizer.normalize(""))
    }

    @Test
    fun `normalize non-Bulgarian international stays as cleaned`() {
        assertEquals("447911123456", PhoneNumberNormalizer.normalize("447911123456"))
    }
}
