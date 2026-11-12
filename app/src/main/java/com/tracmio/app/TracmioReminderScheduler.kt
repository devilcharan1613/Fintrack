package com.tracmio.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object TracmioReminderScheduler {
    const val PREFS = "tracmio_reminders"
    const val KEY_ENABLED = "enabled"
    const val KEY_HOUR = "hour"
    const val KEY_MINUTE = "minute"
    const val KEY_MEME = "meme_mode"
    const val KEY_ALERT_CATEGORY = "alert_category"
    const val KEY_LAST_NOTIFICATION_INDEX = "last_notification_index"
    private const val REQUEST_CODE = 4102

    fun getPrefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun update(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        memeMode: Boolean,
        alertCategory: String = "ALL"
    ) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, safeHour)
            .putInt(KEY_MINUTE, safeMinute)
            .putBoolean(KEY_MEME, memeMode)
            .putString(KEY_ALERT_CATEGORY, alertCategory)
            .apply()
        if (enabled) {
            scheduleNext(context, safeHour, safeMinute, memeMode)
        } else {
            cancel(context)
        }
    }

    fun rescheduleFromPrefs(context: Context) {
        val prefs = getPrefs(context)
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val hour = prefs.getInt(KEY_HOUR, 20)
        val minute = prefs.getInt(KEY_MINUTE, 0)
        val memeMode = prefs.getBoolean(KEY_MEME, true)
        if (enabled) {
            scheduleNext(context, hour, minute, memeMode)
        }
    }

    fun scheduleNext(
        context: Context,
        hour: Int,
        minute: Int,
        memeMode: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = reminderPendingIntent(context, memeMode)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerAtMillis = target.timeInMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(reminderPendingIntent(context, true))
    }

    private fun reminderPendingIntent(context: Context, memeMode: Boolean): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra(KEY_MEME, memeMode)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }
}
