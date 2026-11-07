package com.charan.fintrack

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG = "fintrack-reminder"

    fun calculateNextRun(now: Calendar, hour: Int, minute: Int, includeWeekends: Boolean): Calendar? {
        if (hour !in 0..23 || minute !in 0..59) return null
        val next = now.clone() as Calendar
        next.set(Calendar.HOUR_OF_DAY, hour)
        next.set(Calendar.MINUTE, minute)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1)
        if (!includeWeekends) {
            while (next.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                next.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                next.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return next
    }

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)
        val config = ReminderPrefs.config(context)
        if (!config.optBoolean("enabled", false)) return
        val reminders = config.optJSONArray("reminders") ?: return
        for (index in 0 until reminders.length()) {
            val item = reminders.optJSONObject(index) ?: continue
            if (item.optBoolean("enabled", true)) {
                scheduleNext(context, item.optString("id"), item.optString("time"))
            }
        }
    }

    fun scheduleNext(context: Context, id: String, time: String, appendAfterCurrent: Boolean = false) {
        if (!ReminderPrefs.isEnabled(context, id)) return
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        val config = ReminderPrefs.config(context)
        val next = calculateNextRun(
            Calendar.getInstance(), hour, minute, config.optBoolean("weekends", true)
        ) ?: return
        val delay = (next.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("reminderId" to id, "time" to time))
            .addTag(TAG)
            .build()
        val policy = if (appendAfterCurrent) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork("fintrack-reminder-$id", policy, request)
    }

    fun scheduleSnooze(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(workDataOf("snooze" to true))
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("fintrack-reminder-snooze", ExistingWorkPolicy.REPLACE, request)
    }
}
