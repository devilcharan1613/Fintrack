package com.tracmio.app.data

import android.content.Context
import android.content.SharedPreferences
import com.tracmio.app.data.security.TracmioSecurityEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class CashCountLine(
    val denomType: String, // "NOTE" or "COIN"
    val value: Int,
    val count: Int
)

data class LiquidGlassSettings(
    val themePreset: String = "dark",
    val glassUiStyle: String = "ui1",
    val wallpaperPreset: String = "wp1",
    val backgroundVisibility: Float = 0.45f,
    val wallpaperBlurDp: Float = 8f,
    val glowIntensity: Float = 0.75f,
    val uiScale: Float = 1.0f,
    val deviceMotionEnabled: Boolean = true,
    val animationStyle: Int = 1,
    val customPrimaryHex: String = "#00D6A3",
    val customSecondaryHex: String = "#00A9C7",
    val privacyMaskEnabled: Boolean = false,
    val stealthModeActive: Boolean = false,
    val customWallpaperPath: String? = null,
    val userName: String = "",
    val currencySymbol: String = "₹",
    val hasCompletedOnboarding: Boolean = false,
    val customUpiApps: List<String> = emptyList(),
    val toolsHubIds: List<String> = DEFAULT_TOOLS_HUB_IDS,
    val roundUpEnabled: Boolean = false,
    val roundUpGoalId: Long? = null,
    val roundUpStep: Int = 10,
    val monthlyBackupEnabled: Boolean = false,
    val monthlyBackupDay: Int = 1,
    val monthlyBackupHour: Int = 10,
    val monthlyBackupMinute: Int = 0,
    val cashCountsList: List<CashCountLine> = emptyList(),
    val cashNote: String = ""
) {
    companion object {
        val ALLOWED_CURRENCIES = listOf("₹", "$", "€", "£")
        val DEFAULT_TOOLS_HUB_IDS = listOf(
            "khata",
            "recharge",
            "refunds",
            "wishlist",
            "cashback",
            "price_per_use",
            "round_up",
            "runway",
            "debt_planner",
            "warranty_vault",
            "reports",
            "subscriptions",
            "xray",
            "story",
            "global_search",
            "dna",
            "calendar",
            "reminders",
            "time_machine",
            "data_export",
            "app_lock",
            "permissions",
            "trackers",
            "themes"
        )
    }
}

data class SubscriptionItem(
    val id: String,
    val name: String,
    val amount: Double,
    val billingCycle: String = "Monthly",
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
    val note: String = "",
    val type: String = "Academic"
)

data class CustomReminderItem(
    val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true
)

data class KhataEntryItem(
    val id: String,
    val shopOrPersonName: String,
    val itemTitle: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
) {
    val itemNote: String get() = itemTitle
    val createdAt: Long get() = timestamp
}

data class RechargeTrackerItem(
    val id: String,
    val providerName: String, // Jio, Airtel, Vi, BSNL, WiFi
    val planLabel: String,
    val amount: Double,
    val rechargeDate: String, // YYYY-MM-DD
    val validityDays: Int = 28 // 28, 56, 84
) {
    val operatorName: String get() = providerName
}

data class PendingRefundItem(
    val id: String,
    val sourceName: String,
    val expectedAmount: Double,
    val expectedByLabel: String,
    val isReceived: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val title: String get() = sourceName
    val amount: Double get() = expectedAmount
    val source: String get() = sourceName
    val expectedDateNote: String get() = expectedByLabel
}

data class PersonUpiCard(
    val personId: Long,
    val upiId: String,
    val qrImageUri: String = ""
)

class PreferencesAndThemeStore(context: Context) {
    private val prefs: SharedPreferences = TracmioSecurityEngine.getEncryptedPrefs(context, PREFS_NAME)

    private val _settings = MutableStateFlow(loadSettings().also {
        MoneyPaise.activeCurrencySymbol = it.currencySymbol
    })
    val settings: StateFlow<LiquidGlassSettings> = _settings.asStateFlow()

    private val _subscriptions = MutableStateFlow(loadSubscriptions())
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    private val _academicEvents = MutableStateFlow(loadAcademicEvents())
    val academicEvents: StateFlow<List<AcademicEventItem>> = _academicEvents.asStateFlow()

    private val _customReminders = MutableStateFlow(loadCustomReminders())
    val customReminders: StateFlow<List<CustomReminderItem>> = _customReminders.asStateFlow()

    private val _khataEntries = MutableStateFlow(loadKhataEntries())
    val khataEntries: StateFlow<List<KhataEntryItem>> = _khataEntries.asStateFlow()

    private val _rechargeTrackers = MutableStateFlow(loadRechargeTrackers())
    val rechargeTrackers: StateFlow<List<RechargeTrackerItem>> = _rechargeTrackers.asStateFlow()

    private val _pendingRefunds = MutableStateFlow(loadPendingRefunds())
    val pendingRefunds: StateFlow<List<PendingRefundItem>> = _pendingRefunds.asStateFlow()

    private val _personUpiCards = MutableStateFlow(loadPersonUpiCards())
    val personUpiCards: StateFlow<Map<Long, PersonUpiCard>> = _personUpiCards.asStateFlow()


    private fun loadSettings(): LiquidGlassSettings {
        val themePreset = normalizeThemePreset(prefs.getString(KEY_THEME_PRESET, "dark") ?: "dark")
        val glassUiStyle = prefs.getString(KEY_GLASS_UI, "ui1") ?: "ui1"
        val wallpaperPreset = prefs.getString(KEY_WP_PRESET, "wp1") ?: "wp1"
        val bgVisibility = prefs.getFloat(KEY_BG_VISIBILITY, 0.45f).coerceIn(0f, 1f)
        val blur = prefs.getFloat(KEY_BLUR, 8f).coerceIn(0f, 32f)
        val glow = prefs.getFloat(KEY_GLOW, 0.75f).coerceIn(0.1f, 1f)
        val scale = prefs.getFloat(KEY_SCALE, 1.0f).coerceIn(0.85f, 1.15f)
        val motion = prefs.getBoolean(KEY_MOTION, true)
        val animStyle = prefs.getInt(KEY_ANIM_STYLE, 1).coerceIn(1, 10)
        val customPrimary = prefs.getString(KEY_CUSTOM_PRIMARY, "#00D6A3") ?: "#00D6A3"
        val customSecondary = prefs.getString(KEY_CUSTOM_SECONDARY, "#00A9C7") ?: "#00A9C7"
        val privacy = prefs.getBoolean(KEY_PRIVACY, false)
        val stealth = prefs.getBoolean(KEY_STEALTH, false)
        val wallpaper = prefs.getString(KEY_WALLPAPER, null)
        val userName = prefs.getString(KEY_USER_NAME, "").orEmpty()
        val currencySymbol = prefs.getString(KEY_CURRENCY_SYMBOL, "₹")
            ?.takeIf { it in LiquidGlassSettings.ALLOWED_CURRENCIES } ?: "₹"
        val hasOnboarded = prefs.getBoolean(KEY_ONBOARDED, false)

        val customUpi = prefs.getString(KEY_CUSTOM_UPI_APPS, null)?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }.filter { it.isNotBlank() }
            }.getOrNull()
        }.orEmpty()

        val toolsHubIds = prefs.getString(KEY_TOOLS_HUB_IDS, null)?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }.filter { it.isNotBlank() }
            }.getOrNull()
        }?.ifEmpty { LiquidGlassSettings.DEFAULT_TOOLS_HUB_IDS } ?: LiquidGlassSettings.DEFAULT_TOOLS_HUB_IDS

        val cashCounts = prefs.getString(KEY_CASH_COUNTS_LIST, null)?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val denomType = obj.optString("denomType", "NOTE")
                        val v = obj.optInt("value", 0)
                        val c = obj.optInt("count", 0)
                        if (v > 0 && c > 0) add(CashCountLine(denomType, v, c))
                    }
                }
            }.getOrNull()
        }.orEmpty()

        return LiquidGlassSettings(
            themePreset = themePreset,
            glassUiStyle = glassUiStyle,
            wallpaperPreset = wallpaperPreset,
            backgroundVisibility = bgVisibility,
            wallpaperBlurDp = blur,
            glowIntensity = glow,
            uiScale = scale,
            deviceMotionEnabled = motion,
            animationStyle = animStyle,
            customPrimaryHex = customPrimary,
            customSecondaryHex = customSecondary,
            privacyMaskEnabled = privacy,
            stealthModeActive = stealth,
            customWallpaperPath = wallpaper,
            userName = userName,
            currencySymbol = currencySymbol,
            hasCompletedOnboarding = hasOnboarded,
            customUpiApps = customUpi,
            toolsHubIds = toolsHubIds,
            roundUpEnabled = prefs.getBoolean(KEY_ROUNDUP_ENABLED, false),
            roundUpGoalId = prefs.getLong(KEY_ROUNDUP_GOAL_ID, -1L).takeIf { it > 0L },
            roundUpStep = prefs.getInt(KEY_ROUNDUP_STEP, 10).let { if (it in listOf(10, 50, 100)) it else 10 },
            monthlyBackupEnabled = prefs.getBoolean(KEY_MONTHLY_BACKUP_ENABLED, false),
            monthlyBackupDay = prefs.getInt(KEY_MONTHLY_BACKUP_DAY, 1).coerceIn(1, 28),
            monthlyBackupHour = prefs.getInt(KEY_MONTHLY_BACKUP_HOUR, 10).coerceIn(0, 23),
            monthlyBackupMinute = prefs.getInt(KEY_MONTHLY_BACKUP_MINUTE, 0).coerceIn(0, 59),
            cashCountsList = cashCounts,
            cashNote = prefs.getString(KEY_CASH_NOTE, "").orEmpty()
        )
    }

    private fun normalizeThemePreset(raw: String): String = when (raw) {
        "liquid_glass_dark" -> "dark"
        "cyber_neon" -> "tracmio"
        "emerald_aurora" -> "emerald"
        "deep_sapphire" -> "sapphire"
        "obsidian_gold" -> "obsidian"
        "ruby_sunset" -> "ruby"
        "frosted_light" -> "light"
        else -> raw
    }

    fun updateSettings(transform: (LiquidGlassSettings) -> LiquidGlassSettings) {
        val rawUpdated = transform(_settings.value)
        val safeSymbol = if (rawUpdated.currencySymbol in LiquidGlassSettings.ALLOWED_CURRENCIES) rawUpdated.currencySymbol else "₹"
        val updated = rawUpdated.copy(currencySymbol = safeSymbol)
        MoneyPaise.activeCurrencySymbol = safeSymbol
        _settings.value = updated
        val toolsArr = JSONArray().apply { updated.toolsHubIds.forEach { put(it) } }
        val upiArr = JSONArray().apply { updated.customUpiApps.forEach { put(it) } }
        val cashArr = JSONArray().apply {
            updated.cashCountsList.forEach { line ->
                put(JSONObject().apply {
                    put("denomType", line.denomType)
                    put("value", line.value)
                    put("count", line.count)
                })
            }
        }
        prefs.edit()
            .putString(KEY_THEME_PRESET, updated.themePreset)
            .putString(KEY_GLASS_UI, updated.glassUiStyle)
            .putString(KEY_WP_PRESET, updated.wallpaperPreset)
            .putFloat(KEY_BG_VISIBILITY, updated.backgroundVisibility)
            .putFloat(KEY_BLUR, updated.wallpaperBlurDp)
            .putFloat(KEY_GLOW, updated.glowIntensity)
            .putFloat(KEY_SCALE, updated.uiScale)
            .putBoolean(KEY_MOTION, updated.deviceMotionEnabled)
            .putInt(KEY_ANIM_STYLE, updated.animationStyle)
            .putString(KEY_CUSTOM_PRIMARY, updated.customPrimaryHex)
            .putString(KEY_CUSTOM_SECONDARY, updated.customSecondaryHex)
            .putBoolean(KEY_PRIVACY, updated.privacyMaskEnabled)
            .putBoolean(KEY_STEALTH, updated.stealthModeActive)
            .putString(KEY_WALLPAPER, updated.customWallpaperPath)
            .putString(KEY_USER_NAME, updated.userName)
            .putString(KEY_CURRENCY_SYMBOL, updated.currencySymbol)
            .putBoolean(KEY_ONBOARDED, updated.hasCompletedOnboarding)
            .putString(KEY_CUSTOM_UPI_APPS, upiArr.toString())
            .putString(KEY_TOOLS_HUB_IDS, toolsArr.toString())
            .putBoolean(KEY_ROUNDUP_ENABLED, updated.roundUpEnabled)
            .putLong(KEY_ROUNDUP_GOAL_ID, updated.roundUpGoalId ?: -1L)
            .putInt(KEY_ROUNDUP_STEP, updated.roundUpStep)
            .putBoolean(KEY_MONTHLY_BACKUP_ENABLED, updated.monthlyBackupEnabled)
            .putInt(KEY_MONTHLY_BACKUP_DAY, updated.monthlyBackupDay)
            .putInt(KEY_MONTHLY_BACKUP_HOUR, updated.monthlyBackupHour)
            .putInt(KEY_MONTHLY_BACKUP_MINUTE, updated.monthlyBackupMinute)
            .putString(KEY_CASH_COUNTS_LIST, cashArr.toString())
            .putString(KEY_CASH_NOTE, updated.cashNote)
            .apply()
    }

    fun updateCashCountSession(lines: List<CashCountLine>, note: String) {
        updateSettings { it.copy(cashCountsList = lines, cashNote = note) }
    }

    fun exportPreferencesJson(): JSONObject {
        val s = _settings.value
        return JSONObject().apply {
            put("themePreset", s.themePreset)
            put("glassUiStyle", s.glassUiStyle)
            put("wallpaperPreset", s.wallpaperPreset)
            put("backgroundVisibility", s.backgroundVisibility.toDouble())
            put("wallpaperBlurDp", s.wallpaperBlurDp.toDouble())
            put("glowIntensity", s.glowIntensity.toDouble())
            put("uiScale", s.uiScale.toDouble())
            put("deviceMotionEnabled", s.deviceMotionEnabled)
            put("animationStyle", s.animationStyle)
            put("customPrimaryHex", s.customPrimaryHex)
            put("customSecondaryHex", s.customSecondaryHex)
            put("userName", s.userName)
            put("currencySymbol", s.currencySymbol)
            put("hasCompletedOnboarding", s.hasCompletedOnboarding)
            put("customUpiApps", JSONArray(s.customUpiApps))
            put("toolsHubIds", JSONArray(s.toolsHubIds))
            put("roundUpEnabled", s.roundUpEnabled)
            put("roundUpGoalId", s.roundUpGoalId ?: JSONObject.NULL)
            put("roundUpStep", s.roundUpStep)
            put("monthlyBackupEnabled", s.monthlyBackupEnabled)
            put("monthlyBackupDay", s.monthlyBackupDay)
            put("monthlyBackupHour", s.monthlyBackupHour)
            put("monthlyBackupMinute", s.monthlyBackupMinute)
            put("subscriptions", JSONArray(prefs.getString(KEY_SUBSCRIPTIONS, "[]")))
            put("academicEvents", JSONArray(prefs.getString(KEY_ACADEMIC, "[]")))
            put("customReminders", JSONArray(prefs.getString(KEY_REMINDERS, "[]")))
            put("khataEntries", JSONArray(prefs.getString(KEY_KHATA_ENTRIES, "[]")))
            put("rechargeTrackers", JSONArray(prefs.getString(KEY_RECHARGE_TRACKERS, "[]")))
            put("pendingRefunds", JSONArray(prefs.getString(KEY_PENDING_REFUNDS, "[]")))
            put("personUpiCards", JSONArray(prefs.getString(KEY_PERSON_UPI_CARDS, "[]")))
        }
    }

    fun importPreferencesJson(obj: JSONObject) {
        updateSettings { cur ->
            val tools = obj.optJSONArray("toolsHubIds")?.let { arr ->
                buildList { for (i in 0 until arr.length()) add(arr.optString(i)) }.filter { it.isNotBlank() }
            }?.ifEmpty { cur.toolsHubIds } ?: cur.toolsHubIds
            val upiApps = obj.optJSONArray("customUpiApps")?.let { arr ->
                buildList { for (i in 0 until arr.length()) add(TracmioSecurityEngine.sanitizeInput(arr.optString(i), 30)) }
                    .filter { it.isNotBlank() }
            } ?: cur.customUpiApps
            val importedSym = obj.optString("currencySymbol", cur.currencySymbol)
                .takeIf { it in LiquidGlassSettings.ALLOWED_CURRENCIES } ?: cur.currencySymbol

            cur.copy(
                themePreset = normalizeThemePreset(obj.optString("themePreset", cur.themePreset)),
                glassUiStyle = obj.optString("glassUiStyle", cur.glassUiStyle),
                wallpaperPreset = obj.optString("wallpaperPreset", cur.wallpaperPreset),
                backgroundVisibility = obj.optDouble("backgroundVisibility", cur.backgroundVisibility.toDouble()).toFloat().coerceIn(0f, 1f),
                wallpaperBlurDp = obj.optDouble("wallpaperBlurDp", cur.wallpaperBlurDp.toDouble()).toFloat().coerceIn(0f, 32f),
                glowIntensity = obj.optDouble("glowIntensity", cur.glowIntensity.toDouble()).toFloat().coerceIn(0.1f, 1f),
                uiScale = obj.optDouble("uiScale", cur.uiScale.toDouble()).toFloat().coerceIn(0.85f, 1.15f),
                deviceMotionEnabled = obj.optBoolean("deviceMotionEnabled", cur.deviceMotionEnabled),
                animationStyle = obj.optInt("animationStyle", cur.animationStyle).coerceIn(1, 10),
                customPrimaryHex = obj.optString("customPrimaryHex", cur.customPrimaryHex),
                customSecondaryHex = obj.optString("customSecondaryHex", cur.customSecondaryHex),
                userName = TracmioSecurityEngine.sanitizeInput(obj.optString("userName", cur.userName), 40),
                currencySymbol = importedSym,
                hasCompletedOnboarding = obj.optBoolean("hasCompletedOnboarding", cur.hasCompletedOnboarding),
                customUpiApps = upiApps,
                toolsHubIds = tools,
                roundUpEnabled = obj.optBoolean("roundUpEnabled", cur.roundUpEnabled),
                roundUpGoalId = obj.optLong("roundUpGoalId", -1L).takeIf { it > 0L },
                roundUpStep = obj.optInt("roundUpStep", cur.roundUpStep),
                monthlyBackupEnabled = obj.optBoolean("monthlyBackupEnabled", cur.monthlyBackupEnabled),
                monthlyBackupDay = obj.optInt("monthlyBackupDay", cur.monthlyBackupDay).coerceIn(1, 28),
                monthlyBackupHour = obj.optInt("monthlyBackupHour", cur.monthlyBackupHour).coerceIn(0, 23),
                monthlyBackupMinute = obj.optInt("monthlyBackupMinute", cur.monthlyBackupMinute).coerceIn(0, 59)
            )
        }
        obj.optJSONArray("subscriptions")?.let { arr ->
            val list = parseSubscriptions(arr)
            if (list.isNotEmpty()) saveSubscriptions(list)
        }
        obj.optJSONArray("academicEvents")?.let { arr ->
            val list = parseAcademicEvents(arr)
            if (list.isNotEmpty()) saveAcademicEvents(list)
        }
        obj.optJSONArray("customReminders")?.let { arr ->
            val list = parseReminders(arr)
            if (list.isNotEmpty()) saveCustomReminders(list)
        }
        obj.optJSONArray("khataEntries")?.let { arr ->
            saveKhataEntries(parseKhataEntries(arr))
        }
        obj.optJSONArray("rechargeTrackers")?.let { arr ->
            saveRechargeTrackers(parseRechargeTrackers(arr))
        }
        obj.optJSONArray("pendingRefunds")?.let { arr ->
            savePendingRefunds(parsePendingRefunds(arr))
        }
        obj.optJSONArray("personUpiCards")?.let { arr ->
            savePersonUpiCards(parsePersonUpiCards(arr))
        }
    }

    private fun parseSubscriptions(arr: JSONArray): List<SubscriptionItem> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            add(
                SubscriptionItem(
                    id = TracmioSecurityEngine.sanitizeInput(obj.optString("id"), 60).ifBlank { "sub_$i" },
                    name = TracmioSecurityEngine.sanitizeInput(obj.optString("name", "Subscription"), 80),
                    amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    billingCycle = TracmioSecurityEngine.sanitizeInput(obj.optString("billingCycle", "Monthly"), 20),
                    nextBillingDate = TracmioSecurityEngine.sanitizeInput(obj.optString("nextBillingDate", ""), 20),
                    category = TracmioSecurityEngine.sanitizeInput(obj.optString("category", "Entertainment"), 40),
                    autoRenew = obj.optBoolean("autoRenew", true)
                )
            )
        }
    }

    private fun loadSubscriptions(): List<SubscriptionItem> {
        val raw = prefs.getString(KEY_SUBSCRIPTIONS, null) ?: return defaultSubscriptions()
        return runCatching { parseSubscriptions(JSONArray(raw)) }.getOrDefault(defaultSubscriptions())
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
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("amount", item.amount)
                put("billingCycle", item.billingCycle)
                put("nextBillingDate", item.nextBillingDate)
                put("category", item.category)
                put("autoRenew", item.autoRenew)
            })
        }
        prefs.edit().putString(KEY_SUBSCRIPTIONS, arr.toString()).apply()
    }

    private fun parseAcademicEvents(arr: JSONArray): List<AcademicEventItem> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            add(
                AcademicEventItem(
                    id = TracmioSecurityEngine.sanitizeInput(obj.optString("id"), 60).ifBlank { "acad_$i" },
                    title = TracmioSecurityEngine.sanitizeInput(obj.optString("title", "Event"), 120),
                    date = TracmioSecurityEngine.sanitizeInput(obj.optString("date", ""), 20),
                    feeAmount = obj.optDouble("feeAmount", 0.0).coerceAtLeast(0.0),
                    isPaid = obj.optBoolean("isPaid", false),
                    note = TracmioSecurityEngine.sanitizeInput(obj.optString("note", ""), 200),
                    type = TracmioSecurityEngine.sanitizeInput(obj.optString("type", "Academic"), 30)
                )
            )
        }
    }

    private fun loadAcademicEvents(): List<AcademicEventItem> {
        val raw = prefs.getString(KEY_ACADEMIC, null) ?: return defaultAcademicEvents()
        return runCatching { parseAcademicEvents(JSONArray(raw)) }.getOrDefault(defaultAcademicEvents())
    }

    private fun defaultAcademicEvents(): List<AcademicEventItem> = listOf(
        AcademicEventItem("acad_1", "Semester Tuition Installment", "2026-12-10", 45000.0, false, "Pay online before due date", "Fee"),
        AcademicEventItem("acad_2", "Exam Registration Fee", "2026-11-30", 1800.0, true, "University portal receipt saved", "Exam")
    )

    fun saveAcademicEvents(list: List<AcademicEventItem>) {
        _academicEvents.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("date", item.date)
                put("feeAmount", item.feeAmount)
                put("isPaid", item.isPaid)
                put("note", item.note)
                put("type", item.type)
            })
        }
        prefs.edit().putString(KEY_ACADEMIC, arr.toString()).apply()
    }

    private fun parseReminders(arr: JSONArray): List<CustomReminderItem> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            add(
                CustomReminderItem(
                    id = TracmioSecurityEngine.sanitizeInput(obj.optString("id"), 60).ifBlank { "rem_$i" },
                    title = TracmioSecurityEngine.sanitizeInput(obj.optString("title", "Daily Reminder"), 80),
                    hour = obj.optInt("hour", 21).coerceIn(0, 23),
                    minute = obj.optInt("minute", 0).coerceIn(0, 59),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            )
        }
    }

    private fun loadCustomReminders(): List<CustomReminderItem> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return defaultCustomReminders()
        return runCatching { parseReminders(JSONArray(raw)) }.getOrDefault(defaultCustomReminders())
    }

    private fun defaultCustomReminders(): List<CustomReminderItem> = listOf(
        CustomReminderItem("rem_1", "Night Expense Check-In", 21, 0, true),
        CustomReminderItem("rem_2", "Morning Budget Review", 9, 30, false)
    )

    fun saveCustomReminders(list: List<CustomReminderItem>) {
        _customReminders.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("hour", item.hour)
                put("minute", item.minute)
                put("isEnabled", item.isEnabled)
            })
        }
        prefs.edit().putString(KEY_REMINDERS, arr.toString()).apply()
    }

    private fun parseKhataEntries(arr: JSONArray): List<KhataEntryItem> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            add(
                KhataEntryItem(
                    id = TracmioSecurityEngine.sanitizeInput(obj.optString("id"), 60).ifBlank { "khata_$i" },
                    shopOrPersonName = TracmioSecurityEngine.sanitizeInput(obj.optString("shopOrPersonName", "Kirana / Canteen"), 80),
                    itemTitle = TracmioSecurityEngine.sanitizeInput(obj.optString("itemTitle", "Chai & Snacks"), 120),
                    amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    isSettled = obj.optBoolean("isSettled", false)
                )
            )
        }
    }

    private fun loadKhataEntries(): List<KhataEntryItem> {
        val raw = prefs.getString(KEY_KHATA_ENTRIES, null) ?: return emptyList()
        return runCatching { parseKhataEntries(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    fun saveKhataEntries(list: List<KhataEntryItem>) {
        _khataEntries.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("shopOrPersonName", item.shopOrPersonName)
                put("itemTitle", item.itemTitle)
                put("amount", item.amount)
                put("timestamp", item.timestamp)
                put("isSettled", item.isSettled)
            })
        }
        prefs.edit().putString(KEY_KHATA_ENTRIES, arr.toString()).apply()
    }

    private fun parseRechargeTrackers(arr: JSONArray): List<RechargeTrackerItem> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            add(
                RechargeTrackerItem(
                    id = TracmioSecurityEngine.sanitizeInput(obj.optString("id"), 60).ifBlank { "rech_$i" },
                    providerName = TracmioSecurityEngine.sanitizeInput(obj.optString("providerName", "Jio"), 40),
                    planLabel = TracmioSecurityEngine.sanitizeInput(obj.optString("planLabel", "5G Unlimited"), 80),
                    amount = obj.optDouble("amount", 0.0).coerceAtLeast(0.0),
                    rechargeDate = TracmioSecurityEngine.sanitizeInput(obj.optString("rechargeDate", ""), 20),
                    validityDays = obj.optInt("validityDays", 28).coerceIn(1, 365)
                )
            )
        }
    }

    private fun loadRechargeTrackers(): List<RechargeTrackerItem> {
        val raw = prefs.getString(KEY_RECHARGE_TRACKERS, null) ?: return emptyList()
        return runCatching { parseRechargeTrackers(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    fun saveRechargeTrackers(list: List<RechargeTrackerItem>) {
        _rechargeTrackers.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("providerName", item.providerName)
                put("planLabel", item.planLabel)
                put("amount", item.amount)
                put("rechargeDate", item.rechargeDate)
                put("validityDays", item.validityDays)
            })
        }
        prefs.edit().putString(KEY_RECHARGE_TRACKERS, arr.toString()).apply()
    }

    private fun parsePendingRefunds(arr: JSONArray): List<PendingRefundItem> = buildList {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            add(
                PendingRefundItem(
                    id = TracmioSecurityEngine.sanitizeInput(obj.optString("id"), 60).ifBlank { "ref_$i" },
                    sourceName = TracmioSecurityEngine.sanitizeInput(obj.optString("sourceName", "Amazon / Return"), 100),
                    expectedAmount = obj.optDouble("expectedAmount", 0.0).coerceAtLeast(0.0),
                    expectedByLabel = TracmioSecurityEngine.sanitizeInput(obj.optString("expectedByLabel", "Tomorrow"), 60),
                    isReceived = obj.optBoolean("isReceived", false),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
    }

    private fun loadPendingRefunds(): List<PendingRefundItem> {
        val raw = prefs.getString(KEY_PENDING_REFUNDS, null) ?: return emptyList()
        return runCatching { parsePendingRefunds(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    fun savePendingRefunds(list: List<PendingRefundItem>) {
        _pendingRefunds.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("sourceName", item.sourceName)
                put("expectedAmount", item.expectedAmount)
                put("expectedByLabel", item.expectedByLabel)
                put("isReceived", item.isReceived)
                put("timestamp", item.timestamp)
            })
        }
        prefs.edit().putString(KEY_PENDING_REFUNDS, arr.toString()).apply()
    }

    private fun parsePersonUpiCards(arr: JSONArray): Map<Long, PersonUpiCard> {
        val map = linkedMapOf<Long, PersonUpiCard>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val pid = obj.optLong("personId", -1L)
            if (pid <= 0L) continue
            val upi = TracmioSecurityEngine.sanitizeInput(obj.optString("upiId", ""), 80)
            val qr = TracmioSecurityEngine.sanitizeInput(obj.optString("qrImageUri", ""), 300)
            if (upi.isNotBlank() || qr.isNotBlank()) {
                map[pid] = PersonUpiCard(personId = pid, upiId = upi, qrImageUri = qr)
            }
        }
        return map
    }

    private fun loadPersonUpiCards(): Map<Long, PersonUpiCard> {
        val raw = prefs.getString(KEY_PERSON_UPI_CARDS, null) ?: return emptyMap()
        return runCatching { parsePersonUpiCards(JSONArray(raw)) }.getOrDefault(emptyMap())
    }

    fun savePersonUpiCards(map: Map<Long, PersonUpiCard>) {
        _personUpiCards.value = map
        val arr = JSONArray()
        map.values.forEach { card ->
            arr.put(JSONObject().apply {
                put("personId", card.personId)
                put("upiId", card.upiId)
                put("qrImageUri", card.qrImageUri)
            })
        }
        prefs.edit().putString(KEY_PERSON_UPI_CARDS, arr.toString()).apply()
    }

    fun upsertPersonUpiCard(personId: Long, upiId: String, qrImageUri: String = "") {
        val cleanUpi = TracmioSecurityEngine.sanitizeInput(upiId, 80)
        val cleanQr = TracmioSecurityEngine.sanitizeInput(qrImageUri, 300)
        val updated = _personUpiCards.value.toMutableMap()
        if (cleanUpi.isBlank() && cleanQr.isBlank()) {
            updated.remove(personId)
        } else {
            updated[personId] = PersonUpiCard(personId = personId, upiId = cleanUpi, qrImageUri = cleanQr)
        }
        savePersonUpiCards(updated)
    }

    companion object {
        private const val PREFS_NAME = "tracmio_preferences_and_theme_enc"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_GLASS_UI = "glass_ui_style"
        private const val KEY_WP_PRESET = "wallpaper_preset"
        private const val KEY_BG_VISIBILITY = "background_visibility"
        private const val KEY_BLUR = "wallpaper_blur"
        private const val KEY_GLOW = "glow_intensity"
        private const val KEY_SCALE = "ui_scale"
        private const val KEY_MOTION = "device_motion"
        private const val KEY_ANIM_STYLE = "animation_style"
        private const val KEY_CUSTOM_PRIMARY = "custom_primary_hex"
        private const val KEY_CUSTOM_SECONDARY = "custom_secondary_hex"
        private const val KEY_PRIVACY = "privacy_mask"
        private const val KEY_STEALTH = "stealth_mode_active"
        private const val KEY_WALLPAPER = "custom_wallpaper_path"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_ONBOARDED = "has_completed_onboarding"
        private const val KEY_CUSTOM_UPI_APPS = "custom_upi_apps"
        private const val KEY_TOOLS_HUB_IDS = "tools_hub_ids"
        private const val KEY_ROUNDUP_ENABLED = "roundup_enabled"
        private const val KEY_ROUNDUP_GOAL_ID = "roundup_goal_id"
        private const val KEY_ROUNDUP_STEP = "roundup_step"
        private const val KEY_MONTHLY_BACKUP_ENABLED = "monthly_backup_enabled"
        private const val KEY_MONTHLY_BACKUP_DAY = "monthly_backup_day"
        private const val KEY_MONTHLY_BACKUP_HOUR = "monthly_backup_hour"
        private const val KEY_MONTHLY_BACKUP_MINUTE = "monthly_backup_minute"
        private const val KEY_CASH_COUNTS_LIST = "cash_counts_list"
        private const val KEY_CASH_NOTE = "cash_note"
        private const val KEY_SUBSCRIPTIONS = "subscriptions_list"
        private const val KEY_ACADEMIC = "academic_events_list"
        private const val KEY_REMINDERS = "custom_reminders_list"
        private const val KEY_KHATA_ENTRIES = "khata_entries_list"
        private const val KEY_RECHARGE_TRACKERS = "recharge_trackers_list"
        private const val KEY_PENDING_REFUNDS = "pending_refunds_list"
        private const val KEY_PERSON_UPI_CARDS = "person_upi_cards_list"

        @Volatile
        private var instance: PreferencesAndThemeStore? = null

        fun getInstance(context: Context): PreferencesAndThemeStore {
            return instance ?: synchronized(this) {
                instance ?: PreferencesAndThemeStore(context.applicationContext).also { instance = it }
            }
        }
    }
}

