package com.charan.fintrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.charan.fintrack.REMIND_LATER") {
            ReminderScheduler.scheduleSnooze(context.applicationContext)
        }
    }
}
