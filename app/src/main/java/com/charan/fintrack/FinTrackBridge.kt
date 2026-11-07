package com.charan.fintrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import org.json.JSONObject

class FinTrackBridge(private val activity: MainActivity) {
    @JavascriptInterface
    fun getReminderSettings(): String = ReminderPrefs.config(activity).toString()

    @JavascriptInterface
    fun saveReminderSettings(raw: String): Boolean = try {
        val config = JSONObject(raw)
        ReminderPrefs.save(activity, config)
        ReminderScheduler.scheduleAll(activity)
        if (config.optBoolean("enabled", false)) activity.runOnUiThread { activity.requestReminderPermission() }
        true
    } catch (_: Exception) {
        false
    }

    @JavascriptInterface
    fun requestNotificationPermission() {
        activity.runOnUiThread { activity.requestReminderPermission() }
    }

    @JavascriptInterface
    fun testReminder(): Boolean {
        val config = ReminderPrefs.config(activity)
        if (!ReminderNotifications.canPost(activity, config.optBoolean("sound", true))) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activity.runOnUiThread { activity.requestReminderPermission() }
            }
            return false
        }
        return ReminderNotifications.show(activity, "test-${System.currentTimeMillis()}", "night")
    }
}
