package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quick_templates")
data class QuickTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val icon: String = "⚡",
    val label: String,
    val amount: Double,
    val category: String = "Food & Dining"
)

@Entity(tableName = "cooling_wishlist")
data class WishlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val addedAt: Long = System.currentTimeMillis(),
    val unlockedAt: Long = System.currentTimeMillis() + 24L * 60L * 60L * 1000L,
    val status: String = "COOLING" // COOLING, BOUGHT, SKIPPED
)

@Entity(tableName = "cashback_entries")
data class CashbackEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val source: String,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "value_items")
data class ValueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val cost: Double,
    val uses: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["kind", "isEnabled"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val title: String,
    val kind: String = "CUSTOM", // DAILY, CUSTOM, CALENDAR_EVENT, WARRANTY, MONTHLY_BACKUP
    val hour: Int = 20,
    val minute: Int = 0,
    val dayOfMonth: Int = 1,
    val triggerDate: String = "",
    val isEnabled: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
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
    val customUpiAppsJson: String = "[]",
    val toolsHubIdsJson: String = "[]",
    val roundUpEnabled: Boolean = false,
    val roundUpGoalId: Long? = null,
    val roundUpStep: Int = 10,
    val monthlyBackupEnabled: Boolean = false,
    val monthlyBackupDay: Int = 1,
    val monthlyBackupHour: Int = 10,
    val monthlyBackupMinute: Int = 0,
    val cashCountsJson: String = "[]",
    val cashNote: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
