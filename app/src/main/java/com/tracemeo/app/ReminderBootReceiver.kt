package com.tracemeo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tracemeo.app.data.PreferencesAndThemeStore

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PreferencesAndThemeStore(context.applicationContext)
            if (prefs.isDailyReminderEnabled()) {
                val (hour, minute) = prefs.getDailyReminderTime()
                val settings = prefs.getInitialSettings()
                TracmioReminderScheduler.scheduleDailyReminder(context, hour, minute, settings.userName)
            }
        }
    }
}
