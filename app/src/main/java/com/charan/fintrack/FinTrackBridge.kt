package com.charan.fintrack

import android.webkit.JavascriptInterface
import org.json.JSONObject

class FinTrackBridge(private val activity: MainActivity) {
    @JavascriptInterface
    fun getReminderSettings(): String = ReminderPrefs.config(activity).toString()

    @JavascriptInterface
    fun saveReminderSettings(raw: String): Boolean = try {
        val config = JSONObject(raw)
        ReminderPrefs.save(activity, config)
        ReminderScheduler.scheduleAll(activity)
        if (config.optBoolean("enabled", false)) activity.requestReminderPermission()
        true
    } catch (_: Exception) {
        false
    }

    @JavascriptInterface
    fun requestNotificationPermission() {
        activity.runOnUiThread { activity.requestReminderPermission() }
    }

    @JavascriptInterface
    fun testReminder() {
        ReminderNotifications.show(activity, "test-${System.currentTimeMillis()}", "night")
    }
}
