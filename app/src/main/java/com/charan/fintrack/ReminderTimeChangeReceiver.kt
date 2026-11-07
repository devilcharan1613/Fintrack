package com.charan.fintrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> ReminderScheduler.scheduleAll(context.applicationContext)
        }
    }
}
