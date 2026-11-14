package com.tracemeo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionType = intent.getStringExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION) ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DailyReminderReceiver.EXTRA_QUICK_ACTION, actionType)
        }
        context.startActivity(launchIntent)
    }
}
