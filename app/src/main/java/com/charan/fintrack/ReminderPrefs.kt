package com.charan.fintrack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderPrefs {
    private const val FILE = "fintrack_smart_reminders"
    private const val KEY_CONFIG = "config"
    private const val KEY_SCHEDULE_REVISION = "schedule_revision"
    private val defaults = JSONArray().apply {
        put(JSONObject().put("id", "morning").put("label", "Morning").put("time", "09:00").put("enabled", true))
        put(JSONObject().put("id", "afternoon").put("label", "Afternoon").put("time", "13:30").put("enabled", true))
        put(JSONObject().put("id", "evening").put("label", "Evening").put("time", "18:30").put("enabled", true))
        put(JSONObject().put("id", "night").put("label", "Night").put("time", "21:00").put("enabled", true))
    }

    fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun config(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY_CONFIG, null)
            ?: return defaultConfig()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            defaultConfig()
        }
    }

    private fun defaultConfig() = JSONObject()
        .put("enabled", false)
        .put("memeMode", true)
        .put("weekends", true)
        .put("sound", true)
        .put("reminders", JSONArray(defaults.toString()))

    fun save(context: Context, config: JSONObject): Boolean {
        val reminders = config.optJSONArray("reminders") ?: return false
        if (reminders.length() > 32) return false
        val ids = mutableSetOf<String>()
        val times = mutableSetOf<String>()
        val timePattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        for (index in 0 until reminders.length()) {
            val item = reminders.optJSONObject(index) ?: return false
            val id = item.optString("id")
            val time = item.optString("time")
            if (!Regex("^[A-Za-z0-9_-]{1,80}$").matches(id)) return false
            if (!timePattern.matches(time)) return false
            if (!ids.add(id) || !times.add(time)) return false
        }
        val messages = config.optJSONArray("memeMessages")
        if (messages != null) {
            if (messages.length() > 100) return false
            for (index in 0 until messages.length()) {
                if (messages.optString(index).length > 180) return false
            }
        }
        return prefs(context).edit().putString(KEY_CONFIG, config.toString()).commit()
    }

    fun scheduleRevision(context: Context): Long = prefs(context).getLong(KEY_SCHEDULE_REVISION, 0L)

    fun advanceScheduleRevision(context: Context): Long {
        val next = scheduleRevision(context) + 1L
        prefs(context).edit().putLong(KEY_SCHEDULE_REVISION, next).commit()
        return next
    }

    fun reminders(context: Context): JSONArray =
        config(context).optJSONArray("reminders") ?: JSONArray()

    fun isEnabled(context: Context, id: String): Boolean {
        val config = config(context)
        if (!config.optBoolean("enabled", false)) return false
        val reminders = config.optJSONArray("reminders") ?: return false
        for (index in 0 until reminders.length()) {
            val item = reminders.optJSONObject(index) ?: continue
            if (item.optString("id") == id) return item.optBoolean("enabled", true)
        }
        return false
    }
}
