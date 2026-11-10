package com.charan.fintrack.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses the app's local transaction date/time fields strictly.
 * SimpleDateFormat.parse(String) can accept trailing garbage, so require full
 * input consumption and an exact round trip before persisting a timestamp.
 */
internal fun parseTransactionDateTime(date: String, time: String): Long {
    val input = date + " " + time.ifBlank { "12:00" }
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        isLenient = false
    }
    val position = ParsePosition(0)
    val parsed = format.parse(input, position)
    require(parsed != null && position.index == input.length && format.format(parsed) == input) {
        "Invalid transaction date or time"
    }
    return parsed.time
}
