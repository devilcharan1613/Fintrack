package com.tracemeo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class ReminderNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION).orEmpty()
        NotificationManagerCompat.from(context).cancel(DailyReminderReceiver.NOTIFICATION_ID)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (action.isNotBlank()) {
                putExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION, action)
            }
        }
        context.startActivity(openIntent)
    }
}
