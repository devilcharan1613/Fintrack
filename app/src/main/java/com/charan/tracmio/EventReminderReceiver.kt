package com.charan.tracmio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_CALENDAR_EVENT
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Tracmio Reminder"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have an upcoming financial event."
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = CHANNEL_ID
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val channel = NotificationChannel(
                channelId,
                "Tracmio Events & Calendar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for academic milestones, tuition deadlines, bills, and monthly backups"
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_TYPE, type)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (eventId.hashCode() xor 0x1234),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notifId = (eventId.ifBlank { title }.hashCode() and 0x7FFFFFFF)
        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notifBuilder.build())
        }
    }

    companion object {
        const val CHANNEL_ID = "tracmio_calendar_events"
        const val EXTRA_TYPE = "event_type"
        const val EXTRA_TITLE = "event_title"
        const val EXTRA_MESSAGE = "event_message"
        const val EXTRA_EVENT_ID = "event_id"

        const val TYPE_CALENDAR_EVENT = "CALENDAR_EVENT"
        const val TYPE_MONTHLY_BACKUP = "MONTHLY_BACKUP"
    }
}
