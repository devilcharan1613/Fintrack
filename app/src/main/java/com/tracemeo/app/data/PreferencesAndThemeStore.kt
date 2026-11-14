package com.tracemeo.app.data

import android.content.Context
import com.tracemeo.app.ui.nativeapp.LiquidGlassSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CashCountLine(
    val denomType: String,
    val value: Int,
    val count: Int
)

class PreferencesAndThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences("tracmio_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(getInitialSettings())
    val settingsFlow: StateFlow<LiquidGlassSettings> = _settingsFlow.asStateFlow()

    fun getInitialSettings(): LiquidGlassSettings {
        return LiquidGlassSettings(
            themePreset = prefs.getString("theme", "dark") ?: "dark",
            glassUiStyle = prefs.getString("ui_style", "ui1") ?: "ui1",
            wallpaperPreset = prefs.getString("wallpaper", "wp1") ?: "wp1",
            backgroundVisibility = prefs.getFloat("bg_visibility", 0.45f),
            wallpaperBlurDp = prefs.getFloat("bg_blur", 8f),
            glowIntensity = prefs.getFloat("glow_intensity", 0.75f),
            uiScale = prefs.getFloat("ui_scale", 1.0f),
            deviceMotionEnabled = prefs.getBoolean("device_motion", false),
            animationStyle = prefs.getInt("anim_style", 1),
            customPrimaryHex = prefs.getString("custom_primary", "#00F5D4") ?: "#00F5D4",
            customSecondaryHex = prefs.getString("custom_secondary", "#A855F7") ?: "#A855F7",
            privacyMaskEnabled = prefs.getBoolean("privacy", false),
            privacyMaskActive = prefs.getBoolean("privacy", false),
            stealthModeActive = prefs.getBoolean("stealth", false),
            customWallpaperPath = prefs.getString("custom_wallpaper_path", null),
            userName = prefs.getString("user_name", "Charan") ?: "Charan",
            currencySymbol = prefs.getString("currency", "₹") ?: "₹",
            hasCompletedOnboarding = prefs.getBoolean("onboarding", false),
            roundUpEnabled = prefs.getBoolean("round_up", false)
        )
    }

    fun updateSettings(transform: (LiquidGlassSettings) -> LiquidGlassSettings) {
        val next = transform(_settingsFlow.value)
        prefs.edit()
            .putString("theme", next.themePreset)
            .putString("ui_style", next.glassUiStyle)
            .putString("wallpaper", next.wallpaperPreset)
            .putFloat("bg_visibility", next.backgroundVisibility)
            .putFloat("bg_blur", next.wallpaperBlurDp)
            .putFloat("glow_intensity", next.glowIntensity)
            .putFloat("ui_scale", next.uiScale)
            .putBoolean("device_motion", next.deviceMotionEnabled)
            .putInt("anim_style", next.animationStyle)
            .putString("custom_primary", next.customPrimaryHex)
            .putString("custom_secondary", next.customSecondaryHex)
            .putBoolean("privacy", next.privacyMaskActive)
            .putBoolean("stealth", next.stealthModeActive)
            .putString("custom_wallpaper_path", next.customWallpaperPath)
            .putString("user_name", next.userName)
            .putString("currency", next.currencySymbol)
            .putBoolean("onboarding", next.hasCompletedOnboarding)
            .putBoolean("round_up", next.roundUpEnabled)
            .apply()
        _settingsFlow.value = next
    }

    fun updateTheme(preset: String) = updateSettings { it.copy(themePreset = preset) }
    fun updatePrivacyMask(active: Boolean) = updateSettings { it.copy(privacyMaskEnabled = active, privacyMaskActive = active) }
    fun updateStealthMode(active: Boolean) = updateSettings { it.copy(stealthModeActive = active) }
    fun completeOnboarding(name: String, currency: String) =
        updateSettings { it.copy(userName = name, currencySymbol = currency, hasCompletedOnboarding = true) }

    fun hasSeededDefaults(): Boolean = prefs.getBoolean("seeded_defaults_v1", false)
    fun markSeededDefaults() = prefs.edit().putBoolean("seeded_defaults_v1", true).apply()

    fun getQuickSpendChips(): List<QuickSpendChip> {
        val raw = prefs.getString("quick_spend_chips", null)
        if (raw == null) {
            return listOf(
                QuickSpendChip("q1", "☕", "Tea", 20.0, "Food & Chai"),
                QuickSpendChip("q2", "🚌", "Auto", 50.0, "Transport & Travel"),
                QuickSpendChip("q3", "🍽️", "Lunch", 120.0, "Food & Chai"),
                QuickSpendChip("q4", "📱", "Recharge", 199.0, "SIM, WiFi & Bills")
            )
        }
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                QuickSpendChip(
                    id = o.optString("id", "q_$idx"),
                    emoji = o.optString("emoji", "⚡"),
                    title = o.optString("title", "Spend"),
                    amount = o.optDouble("amount", 0.0),
                    category = o.optString("category", "Other")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveQuickSpendChips(chips: List<QuickSpendChip>) {
        val arr = JSONArray()
        chips.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("emoji", c.emoji)
                put("title", c.title)
                put("amount", c.amount)
                put("category", c.category)
            })
        }
        prefs.edit().putString("quick_spend_chips", arr.toString()).apply()
    }

    fun getPendingRefunds(): List<PendingRefundItem> {
        val raw = prefs.getString("pending_refunds", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                PendingRefundItem(
                    id = o.optString("id", "ref_$idx"),
                    title = o.optString("title", "Refund"),
                    source = o.optString("source", "Store / App"),
                    amount = o.optDouble("amount", 0.0),
                    expectedDate = o.optString("expectedDate", "Soon"),
                    isReceived = o.optBoolean("isReceived", false)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun savePendingRefunds(items: List<PendingRefundItem>) {
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("source", r.source)
                put("amount", r.amount)
                put("expectedDate", r.expectedDate)
                put("isReceived", r.isReceived)
            })
        }
        prefs.edit().putString("pending_refunds", arr.toString()).apply()
    }

    fun getPinnedToolIds(): List<String> {
        val raw = prefs.getString("pinned_tool_ids", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun savePinnedToolIds(ids: List<String>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString("pinned_tool_ids", arr.toString()).apply()
    }

    fun isDailyReminderEnabled(): Boolean = prefs.getBoolean("daily_reminder_enabled", false)
    fun getDailyReminderTime(): Pair<Int, Int> =
        prefs.getInt("daily_reminder_hour", 21) to prefs.getInt("daily_reminder_minute", 0)

    fun saveDailyReminderConfig(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean("daily_reminder_enabled", enabled)
            .putInt("daily_reminder_hour", hour)
            .putInt("daily_reminder_minute", minute)
            .apply()
    }
}
