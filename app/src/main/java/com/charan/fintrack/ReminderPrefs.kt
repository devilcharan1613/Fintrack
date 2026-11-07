package com.charan.fintrack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderPrefs {
    private const val FILE = "fintrack_smart_reminders"
    private const val KEY_CONFIG = "config"
    private val defaults = JSONArray().apply {
        put(JSONObject().put("id","morning").put("label","Morning").put("time","09:00").put("enabled",true))
        put(JSONObject().put("id","afternoon").put("label","Afternoon").put("time","13:30").put("enabled",true))
        put(JSONObject().put("id","evening").put("label","Evening").put("time","18:30").put("enabled",true))
        put(JSONObject().put("id","night").put("label","Night").put("time","21:00").put("enabled",true))
    }

    fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    fun config(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY_CONFIG, null) ?: return JSONObject().put("enabled",false)
            .put("memeMode",true).put("weekends",true).put("sound",true).put("reminders",defaults)
        return try { JSONObject(raw) } catch (_: Exception) {
            JSONObject().put("enabled",false).put("memeMode",true).put("weekends",true).put("sound",true).put("reminders",defaults)
        }
    }
    fun save(context: Context, config: JSONObject) {
        prefs(context).edit().putString(KEY_CONFIG, config.toString()).apply()
    }
    fun reminders(context: Context): JSONArray = config(context).optJSONArray("reminders") ?: JSONArray()
    fun isEnabled(context: Context, id: String): Boolean {
        val c=config(context)
        if (!c.optBoolean("enabled", false)) return false
        val arr=c.optJSONArray("reminders") ?: return false
        for(i in 0 until arr.length()) {
            val item=arr.optJSONObject(i) ?: continue
            if(item.optString("id")==id) return item.optBoolean("enabled",true)
        }
        return false
    }
}