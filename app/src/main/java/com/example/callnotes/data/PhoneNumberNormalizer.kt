package com.example.callnotes.data

object PhoneNumberNormalizer {
    fun normalize(raw: String): String {
        val cleaned = raw.replace("[^\\d+]".toRegex(), "")
        return if (cleaned.startsWith("0") && cleaned.length == 10) "+359" + cleaned.substring(1) else if (cleaned.startsWith("359") && !cleaned.startsWith("+")) "+" + cleaned else cleaned
    }
}
