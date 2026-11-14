package com.tracemeo.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object TracmioReminderScheduler {
    const val CHANNEL_ID = "tracmio_daily"
    const val CHANNEL_NAME = "Tracmio Offline Reminders"
    private const val DAILY_REQ_CODE = 1613
    private const val MONTHLY_BACKUP_REQ_CODE = 1614

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "100% offline expense check-ins, bill alerts, and monthly backup reminders"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleDailyReminder(
        context: Context,
        hour: Int = 21,
        minute: Int = 0,
        userName: String = "Charan"
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra("user_name", userName)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            DAILY_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            DAILY_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun scheduleEventReminder(
        context: Context,
        title: String,
        message: String,
        triggerAtMillis: Long,
        eventId: String
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("event_id", eventId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1000L), pi)
    }

    fun scheduleMonthlyBackupReminder(
        context: Context,
        enabled: Boolean,
        dayOfMonth: Int = 1,
        hour: Int = 20,
        minute: Int = 0
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra("title", "Monthly Encrypted Vault Backup")
            putExtra("message", "Protect your offline Tracmio ledger by saving an encrypted JSON backup.")
            putExtra("event_id", "monthly_backup")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            MONTHLY_BACKUP_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            alarmManager.cancel(pi)
            return
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, 28))
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY * 30L,
            pi
        )
    }
}
