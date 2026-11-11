package com.charan.tracmio.ui.nativeapp

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

data class TracmioUserPreferences(
    val isWelcomed: Boolean = false,
    val userName: String = "",
    val defaultCurrency: String = "INR",
    val bgThemeId: String = "bg1",
    val glassUiId: String = "ui1",
    val customPrimaryHex: String = "",
    val customSecondaryHex: String = "",
    val customBgImagePath: String = "",
    val bgPhotoOpacity: Float = 0.45f,
    val uiScale: Float = 1.0f,
    val upiApps: List<String> = DEFAULT_UPI_APPS,
    val biometricEnabled: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderMemeMode: Boolean = true,
    val reminderAlertCategory: String = "ALL"
) {
    val currencySymbol: String
        get() = currencySymbolFor(defaultCurrency)

    companion object {
        val DEFAULT_UPI_APPS = listOf("GPay", "PhonePe", "Paytm", "BHIM", "Amazon Pay", "CRED")

        fun currencySymbolFor(code: String): String = when (code.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "AED" -> "AED "
            else -> "₹"
        }
    }
}

class TracmioPreferencesStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val reminderPrefs = appContext.getSharedPreferences("tracmio_reminders", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<TracmioUserPreferences> = _state.asStateFlow()

    private fun load(): TracmioUserPreferences {
        val upiJson = prefs.getString(KEY_UPI_APPS, null)
        val parsedUpi = if (!upiJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(upiJson)
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }.getOrDefault(TracmioUserPreferences.DEFAULT_UPI_APPS)
        } else {
            TracmioUserPreferences.DEFAULT_UPI_APPS
        }.ifEmpty { TracmioUserPreferences.DEFAULT_UPI_APPS }

        return TracmioUserPreferences(
            isWelcomed = prefs.getBoolean(KEY_WELCOMED, false),
            userName = prefs.getString(KEY_USER_NAME, "") ?: "",
            defaultCurrency = prefs.getString(KEY_CURRENCY, "INR") ?: "INR",
            bgThemeId = prefs.getString(KEY_BG_THEME, "bg1") ?: "bg1",
            glassUiId = prefs.getString(KEY_GLASS_UI, "ui1") ?: "ui1",
            customPrimaryHex = prefs.getString(KEY_CUSTOM_PRIMARY, "") ?: "",
            customSecondaryHex = prefs.getString(KEY_CUSTOM_SECONDARY, "") ?: "",
            customBgImagePath = prefs.getString(KEY_BG_IMAGE_PATH, "") ?: "",
            bgPhotoOpacity = prefs.getFloat(KEY_BG_OPACITY, 0.45f).coerceIn(0f, 1f),
            uiScale = prefs.getFloat(KEY_UI_SCALE, 1.0f).coerceIn(0.8f, 1.3f),
            upiApps = parsedUpi,
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
            reminderEnabled = reminderPrefs.getBoolean("enabled", true),
            reminderHour = reminderPrefs.getInt("hour", 20),
            reminderMinute = reminderPrefs.getInt("minute", 0),
            reminderMemeMode = reminderPrefs.getBoolean("meme_mode", true),
            reminderAlertCategory = reminderPrefs.getString("alert_category", "ALL") ?: "ALL"
        )
    }

    fun completeWelcome(name: String, currency: String) {
        val safeCurrency = currency.ifBlank { "INR" }
        prefs.edit()
            .putBoolean(KEY_WELCOMED, true)
            .putString(KEY_USER_NAME, name.trim())
            .putString(KEY_CURRENCY, safeCurrency)
            .apply()
        _state.value = load()
    }

    fun reopenWelcome() {
        prefs.edit().putBoolean(KEY_WELCOMED, false).apply()
        _state.value = load()
    }

    fun setBgTheme(bgId: String) {
        prefs.edit()
            .putString(KEY_BG_THEME, bgId)
            .putString(KEY_CUSTOM_PRIMARY, "")
            .putString(KEY_CUSTOM_SECONDARY, "")
            .apply()
        _state.value = load()
    }

    fun setGlassUi(uiId: String) {
        prefs.edit().putString(KEY_GLASS_UI, uiId).apply()
        _state.value = load()
    }

    fun setCustomNeonColors(primaryHex: String, secondaryHex: String) {
        prefs.edit()
            .putString(KEY_CUSTOM_PRIMARY, primaryHex.trim())
            .putString(KEY_CUSTOM_SECONDARY, secondaryHex.trim())
            .apply()
        _state.value = load()
    }

    fun setCustomBgImagePath(path: String) {
        prefs.edit().putString(KEY_BG_IMAGE_PATH, path).apply()
        _state.value = load()
    }

    fun setBgPhotoOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_BG_OPACITY, opacity.coerceIn(0f, 1f)).apply()
        _state.value = load()
    }

    fun setUiScale(scale: Float) {
        prefs.edit().putFloat(KEY_UI_SCALE, scale.coerceIn(0.8f, 1.3f)).apply()
        _state.value = load()
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply()
        _state.value = load()
    }

    fun addCustomUpiApp(appName: String) {
        val trimmed = appName.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value.upiApps.toMutableList()
        if (current.none { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            val arr = JSONArray()
            current.forEach { arr.put(it) }
            prefs.edit().putString(KEY_UPI_APPS, arr.toString()).apply()
            _state.value = load()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _state.value = load()
    }

    fun updateReminderState(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        memeMode: Boolean,
        alertCategory: String
    ) {
        reminderPrefs.edit()
            .putBoolean("enabled", enabled)
            .putInt("hour", hour.coerceIn(0, 23))
            .putInt("minute", minute.coerceIn(0, 59))
            .putBoolean("meme_mode", memeMode)
            .putString("alert_category", alertCategory)
            .apply()
        _state.value = load()
    }

    fun resetAllPreferencesForNewUser() {
        prefs.edit()
            .putBoolean(KEY_WELCOMED, false)
            .putString(KEY_USER_NAME, "")
            .apply()
        _state.value = load()
    }

    companion object {
        const val PREFS_NAME = "tracmio_native_preferences"
        private const val KEY_WELCOMED = "tracmio_welcomed"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_CURRENCY = "default_currency"
        private const val KEY_BG_THEME = "bg_theme_id"
        private const val KEY_GLASS_UI = "glass_ui_id"
        private const val KEY_CUSTOM_PRIMARY = "custom_primary_hex"
        private const val KEY_CUSTOM_SECONDARY = "custom_secondary_hex"
        private const val KEY_BG_IMAGE_PATH = "custom_bg_image_path"
        private const val KEY_BG_OPACITY = "bg_photo_opacity"
        private const val KEY_UI_SCALE = "ui_scale"
        private const val KEY_UPI_APPS = "custom_upi_apps"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
