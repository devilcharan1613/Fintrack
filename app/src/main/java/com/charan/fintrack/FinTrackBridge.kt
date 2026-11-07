package com.charan.fintrack

import android.webkit.JavascriptInterface
import org.json.JSONObject

class FinTrackBridge(private val activity: MainActivity) {
    @JavascriptInterface fun getReminderSettings(): String = ReminderPrefs.config(activity).toString()
    @JavascriptInterface fun saveReminderSettings(raw: String): Boolean {
        return try {
            val config=JSONObject(raw)
            ReminderPrefs.save(activity,config)
            ReminderScheduler.scheduleAll(activity)
            true
        } catch (_: Exception) { false }
    }
    @JavascriptInterface fun requestNotificationPermission() { activity.requestReminderPermission() }
    @JavascriptInterface fun testReminder() { ReminderNotifications.show(activity,"test-${System.currentTimeMillis()}","night") }
}