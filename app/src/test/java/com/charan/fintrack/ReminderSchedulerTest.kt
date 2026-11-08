package com.charan.fintrack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderSchedulerTest {
    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun rejectsInvalidHoursAndMinutes() {
        val now = calendar(2026, Calendar.NOVEMBER, 7, 8, 0)
        assertNull(ReminderScheduler.calculateNextRun(now, 24, 0, true))
        assertNull(ReminderScheduler.calculateNextRun(now, 9, 60, true))
    }

    @Test
    fun schedulesLaterTodayWhenReminderTimeHasNotPassed() {
        val now = calendar(2026, Calendar.NOVEMBER, 7, 8, 0)
        val next = ReminderScheduler.calculateNextRun(now, 9, 30, true)!!
        assertEquals(2026, next.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, next.get(Calendar.MONTH))
        assertEquals(7, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, next.get(Calendar.MINUTE))
    }

    @Test
    fun schedulesTomorrowWhenReminderTimeHasPassedOrIsEqual() {
        val now = calendar(2026, Calendar.NOVEMBER, 7, 9, 0)
        val next = ReminderScheduler.calculateNextRun(now, 9, 0, true)!!
        assertEquals(8, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun skipsSaturdayAndSundayWhenWeekendsAreDisabled() {
        val now = calendar(2026, Calendar.NOVEMBER, 7, 8, 0)
        val next = ReminderScheduler.calculateNextRun(now, 9, 0, false)!!
        assertEquals(Calendar.MONDAY, next.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY))
    }
}
