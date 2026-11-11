package com.charan.fintrack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderTimeTest {
    @Test
    fun reminderUsesTodayWhenConfiguredTimeIsStillAhead() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 4, 18, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expected = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 15)
        }.timeInMillis

        assertEquals(expected, ReminderTime.nextTriggerAt(now.timeInMillis, 20, 15))
    }

    @Test
    fun reminderMovesToTomorrowWhenConfiguredTimeHasPassed() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 4, 21, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expected = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 45)
        }.timeInMillis

        assertEquals(expected, ReminderTime.nextTriggerAt(now.timeInMillis, 7, 45))
    }

    @Test
    fun reminderRejectsInvalidTime() {
        val now = System.currentTimeMillis()
        val hourError = runCatching { ReminderTime.nextTriggerAt(now, 24, 0) }.exceptionOrNull()
        val minuteError = runCatching { ReminderTime.nextTriggerAt(now, 8, 60) }.exceptionOrNull()
        assertTrue(hourError is IllegalArgumentException)
        assertTrue(minuteError is IllegalArgumentException)
    }
}
