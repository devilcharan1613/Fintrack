package com.tracmio.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class LiquidGlassSettings(
    val themePreset: String = "liquid_glass_dark",
    val wallpaperBlurDp: Float = 18f,
    val glowIntensity: Float = 0.75f,
    val uiScale: Float = 1.0f,
    val deviceMotionEnabled: Boolean = true,
    val animationStyle: Int = 1, // 1 to 10
    val privacyMaskEnabled: Boolean = false,
    val customWallpaperPath: String? = null,
    val cashDenominations: Map<Int, Int> = mapOf(
        500 to 0, 200 to 0, 100 to 0, 50 to 0, 20 to 0, 10 to 0, 5 to 0, 2 to 0, 1 to 0
    )
)

data class SubscriptionItem(
    val id: String,
    val name: String,
    val amount: Double,
    val billingCycle: String = "Monthly", // "Monthly", "Yearly", "Weekly"
    val nextBillingDate: String = "",
    val category: String = "Entertainment",
    val autoRenew: Boolean = true
)

data class AcademicEventItem(
    val id: String,
    val title: String,
    val date: String,
    val feeAmount: Double = 0.0,
    val isPaid: Boolean = false,
    val note: String = ""
)

data class CustomReminderItem(
    val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isMeme: Boolean = true,
    val isEnabled: Boolean = true
)

class PreferencesAndThemeStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LiquidGlassSettings> = _settings.asStateFlow()

    private val _subscriptions = MutableStateFlow(loadSubscriptions())
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    private val _academicEvents = MutableStateFlow(loadAcademicEvents())
    val academicEvents: StateFlow<List<AcademicEventItem>> = _academicEvents.asStateFlow()

    private val _customReminders = MutableStateFlow(loadCustomReminders())
    val customReminders: StateFlow<List<CustomReminderItem>> = _customReminders.asStateFlow()

    private fun loadSettings(): LiquidGlassSettings {
        val themePreset = prefs.getString(KEY_THEME_PRESET, "liquid_glass_dark") ?: "liquid_glass_dark"
        val blur = prefs.getFloat(KEY_BLUR, 18f)
        val glow = prefs.getFloat(KEY_GLOW, 0.75f)
        val scale = prefs.getFloat(KEY_SCALE, 1.0f)
        val motion = prefs.getBoolean(KEY_MOTION, true)
        val animStyle = prefs.getInt(KEY_ANIM_STYLE, 1).coerceIn(1, 10)
        val privacy = prefs.getBoolean(KEY_PRIVACY, false)
        val wallpaper = prefs.getString(KEY_WALLPAPER, null)

        val denomJson = prefs.getString(KEY_DENOMINATIONS, null)
        val denoms = if (!denomJson.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(denomJson)
                listOf(500, 200, 100, 50, 20, 10, 5, 2, 1).associateWith { obj.optInt(it.toString(), 0) }
            }.getOrDefault(defaultDenominations())
        } else {
            defaultDenominations()
        }

        return LiquidGlassSettings(
            themePreset = themePreset,
            wallpaperBlurDp = blur,
            glowIntensity = glow,
            uiScale = scale,
            deviceMotionEnabled = motion,
            animationStyle = animStyle,
            privacyMaskEnabled = privacy,
            customWallpaperPath = wallpaper,
            cashDenominations = denoms
        )
    }

    private fun defaultDenominations() = mapOf(
        500 to 0, 200 to 0, 100 to 0, 50 to 0, 20 to 0, 10 to 0, 5 to 0, 2 to 0, 1 to 0
    )

    fun updateSettings(transform: (LiquidGlassSettings) -> LiquidGlassSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit()
            .putString(KEY_THEME_PRESET, updated.themePreset)
            .putFloat(KEY_BLUR, updated.wallpaperBlurDp)
            .putFloat(KEY_GLOW, updated.glowIntensity)
            .putFloat(KEY_SCALE, updated.uiScale)
            .putBoolean(KEY_MOTION, updated.deviceMotionEnabled)
            .putInt(KEY_ANIM_STYLE, updated.animationStyle)
            .putBoolean(KEY_PRIVACY, updated.privacyMaskEnabled)
            .putString(KEY_WALLPAPER, updated.customWallpaperPath)
            .putString(KEY_DENOMINATIONS, JSONObject(updated.cashDenominations).toString())
            .apply()
    }

    fun updateCashDenominations(map: Map<Int, Int>) {
        updateSettings { it.copy(cashDenominations = map) }
    }

    private fun loadSubscriptions(): List<SubscriptionItem> {
        val raw = prefs.getString(KEY_SUBSCRIPTIONS, null) ?: return defaultSubscriptions()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        SubscriptionItem(
                            id = obj.optString("id", System.currentTimeMillis().toString()),
                            name = obj.optString("name", "Subscription"),
                            amount = obj.optDouble("amount", 0.0),
                            billingCycle = obj.optString("billingCycle", "Monthly"),
                            nextBillingDate = obj.optString("nextBillingDate", ""),
                            category = obj.optString("category", "Entertainment"),
                            autoRenew = obj.optBoolean("autoRenew", true)
                        )
                    )
                }
            }
        }.getOrDefault(defaultSubscriptions())
    }

    private fun defaultSubscriptions(): List<SubscriptionItem> = listOf(
        SubscriptionItem("sub_1", "Netflix Premium", 649.0, "Monthly", "2026-12-01", "Entertainment", true),
        SubscriptionItem("sub_2", "Spotify Individual", 119.0, "Monthly", "2026-11-28", "Entertainment", true),
        SubscriptionItem("sub_3", "Amazon Prime", 1499.0, "Yearly", "2027-01-15", "Shopping", true)
    )

    fun saveSubscriptions(list: List<SubscriptionItem>) {
        _subscriptions.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("amount", item.amount)
                    put("billingCycle", item.billingCycle)
                    put("nextBillingDate", item.nextBillingDate)
                    put("category", item.category)
                    put("autoRenew", item.autoRenew)
                }
            )
        }
        prefs.edit().putString(KEY_SUBSCRIPTIONS, arr.toString()).apply()
    }

    private fun loadAcademicEvents(): List<AcademicEventItem> {
        val raw = prefs.getString(KEY_ACADEMIC, null) ?: return defaultAcademicEvents()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        AcademicEventItem(
                            id = obj.optString("id", System.currentTimeMillis().toString()),
                            title = obj.optString("title", "Event"),
                            date = obj.optString("date", ""),
                            feeAmount = obj.optDouble("feeAmount", 0.0),
                            isPaid = obj.optBoolean("isPaid", false),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }
        }.getOrDefault(defaultAcademicEvents())
    }

    private fun defaultAcademicEvents(): List<AcademicEventItem> = listOf(
        AcademicEventItem("acad_1", "Semester Tuition Installment", "2026-12-10", 45000.0, false, "Pay online before due date"),
        AcademicEventItem("acad_2", "Exam Registration Fee", "2026-11-30", 1800.0, true, "University portal receipt saved")
    )

    fun saveAcademicEvents(list: List<AcademicEventItem>) {
        _academicEvents.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("date", item.date)
                    put("feeAmount", item.feeAmount)
                    put("isPaid", item.isPaid)
                    put("note", item.note)
                }
            )
        }
        prefs.edit().putString(KEY_ACADEMIC, arr.toString()).apply()
    }

    private fun loadCustomReminders(): List<CustomReminderItem> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return defaultCustomReminders()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CustomReminderItem(
                            id = obj.optString("id", System.currentTimeMillis().toString()),
                            title = obj.optString("title", "Daily Reminder"),
                            hour = obj.optInt("hour", 21),
                            minute = obj.optInt("minute", 0),
                            isMeme = obj.optBoolean("isMeme", true),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
            }
        }.getOrDefault(defaultCustomReminders())
    }

    private fun defaultCustomReminders(): List<CustomReminderItem> = listOf(
        CustomReminderItem("rem_1", "Night Expense Log", 21, 0, true, true),
        CustomReminderItem("rem_2", "Morning Budget Review", 9, 30, false, false)
    )

    fun saveCustomReminders(list: List<CustomReminderItem>) {
        _customReminders.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("hour", item.hour)
                    put("minute", item.minute)
                    put("isMeme", item.isMeme)
                    put("isEnabled", item.isEnabled)
                }
            )
        }
        prefs.edit().putString(KEY_REMINDERS, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "tracmio_preferences_and_theme"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_BLUR = "wallpaper_blur"
        private const val KEY_GLOW = "glow_intensity"
        private const val KEY_SCALE = "ui_scale"
        private const val KEY_MOTION = "device_motion"
        private const val KEY_ANIM_STYLE = "animation_style"
        private const val KEY_PRIVACY = "privacy_mask"
        private const val KEY_WALLPAPER = "custom_wallpaper_path"
        private const val KEY_DENOMINATIONS = "cash_denominations"
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"
        private const val KEY_ACADEMIC = "academic_events_list"
        private const val KEY_REMINDERS = "custom_reminders_list"

        @Volatile
        private var instance: PreferencesAndThemeStore? = null

        fun getInstance(context: Context): PreferencesAndThemeStore {
            return instance ?: synchronized(this) {
                instance ?: PreferencesAndThemeStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
