package com.charan.tracmio.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TransactionDateTime {
    fun formatDateInput(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    fun formatTimeInput(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.US).format(Date(timestamp))

    fun parseDateAndTime(
        dateText: String,
        timeText: String,
        fallbackMillis: Long = System.currentTimeMillis()
    ): Long {
        val dateParts = dateText.trim().split("-")
        val timeParts = timeText.trim().split(":")
        if (dateParts.size != 3) return fallbackMillis
        val year = dateParts[0].toIntOrNull() ?: return fallbackMillis
        val month = dateParts[1].toIntOrNull() ?: return fallbackMillis
        val day = dateParts[2].toIntOrNull() ?: return fallbackMillis
        val hour = timeParts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val minute = timeParts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        return Calendar.getInstance().apply {
            timeInMillis = fallbackMillis
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfDayMillis(dateText: String): Long? {
        val parts = dateText.trim().split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, (m - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, d.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDayMillis(dateText: String): Long? {
        val start = startOfDayMillis(dateText) ?: return null
        return Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
