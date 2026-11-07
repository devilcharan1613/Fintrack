package com.charan.fintrack

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("reminderId")
        val snooze = inputData.getBoolean("snooze", false)
        if (snooze) {
            if (ReminderPrefs.config(applicationContext).optBoolean("enabled", false)) {
                ReminderNotifications.show(applicationContext, "snooze", "night")
            }
            return Result.success()
        }
        if (id.isNullOrBlank() || !ReminderPrefs.isEnabled(applicationContext, id)) return Result.success()
        val config = ReminderPrefs.config(applicationContext)
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (!config.optBoolean("weekends", true) &&
            (day == Calendar.SATURDAY || day == Calendar.SUNDAY)) {
            reschedule(id)
            return Result.success()
        }
        ReminderNotifications.show(applicationContext, id, id)
        reschedule(id)
        return Result.success()
    }

    private fun reschedule(id: String) {
        val reminders = ReminderPrefs.reminders(applicationContext)
        for (index in 0 until reminders.length()) {
            val item = reminders.optJSONObject(index) ?: continue
            if (item.optString("id") == id) {
                ReminderScheduler.scheduleNext(
                    applicationContext, id, item.optString("time"), appendAfterCurrent = true
                )
                return
            }
        }
    }
}
