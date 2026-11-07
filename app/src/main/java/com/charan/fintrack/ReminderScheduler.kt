package com.charan.fintrack

import android.content.Context
import androidx.work.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG = "fintrack-reminder"
    fun scheduleAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        val c=ReminderPrefs.config(context)
        if(!c.optBoolean("enabled",false)) return
        val arr=c.optJSONArray("reminders") ?: return
        for(i in 0 until arr.length()) {
            val item=arr.optJSONObject(i) ?: continue
            if(item.optBoolean("enabled",true)) scheduleNext(context,item.optString("id"),item.optString("time"))
        }
    }
    fun scheduleNext(context: Context, id: String, time: String) {
        if(!ReminderPrefs.isEnabled(context,id)) return
        val parts=time.split(":")
        if(parts.size!=2) return
        val hour=parts[0].toIntOrNull() ?: return
        val minute=parts[1].toIntOrNull() ?: return
        val now=LocalDateTime.now()
        var next=now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if(!next.isAfter(now)) next=next.plusDays(1)
        val cfg=ReminderPrefs.config(context)
        if(!cfg.optBoolean("weekends",true)) {
            while(next.dayOfWeek.value>=6) next=next.plusDays(1)
        }
        val delay=next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()-System.currentTimeMillis()
        val request=OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.coerceAtLeast(1000),TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("reminderId" to id,"time" to time))
            .addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork("fintrack-reminder-$id",ExistingWorkPolicy.REPLACE,request)
    }
    fun scheduleSnooze(context: Context) {
        val req=OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(15,TimeUnit.MINUTES)
            .setInputData(workDataOf("snooze" to true))
            .addTag(TAG).build()
        WorkManager.getInstance(context).enqueue(req)
    }
}