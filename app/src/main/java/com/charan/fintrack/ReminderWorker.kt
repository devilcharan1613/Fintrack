package com.charan.fintrack

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val id=inputData.getString("reminderId")
        val snooze=inputData.getBoolean("snooze",false)
        if(snooze) {
            ReminderNotifications.show(applicationContext,"snooze","night")
            return Result.success()
        }
        if(id.isNullOrBlank() || !ReminderPrefs.isEnabled(applicationContext,id)) return Result.success()
        val cfg=ReminderPrefs.config(applicationContext)
        if(!cfg.optBoolean("weekends",true) && java.time.LocalDate.now().dayOfWeek.value>=6) {
            val arr=ReminderPrefs.reminders(applicationContext)
            for(i in 0 until arr.length()) {
                val item=arr.optJSONObject(i) ?: continue
                if(item.optString("id")==id) ReminderScheduler.scheduleNext(applicationContext,id,item.optString("time"))
            }
            return Result.success()
        }
        ReminderNotifications.show(applicationContext,id,id)
        val arr=ReminderPrefs.reminders(applicationContext)
        for(i in 0 until arr.length()) {
            val item=arr.optJSONObject(i) ?: continue
            if(item.optString("id")==id) ReminderScheduler.scheduleNext(applicationContext,id,item.optString("time"))
        }
        return Result.success()
    }
}