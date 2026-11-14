package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val amount: Double,
    val amountPaise: Long = Math.round(amount * 100.0),
    val billingCycle: String = "Monthly", // Monthly, Yearly, 28d, 56d, 84d
    val nextDueDateMillis: Long = System.currentTimeMillis(),
    val category: String = "SIM, WiFi & Bills",
    val isSimOrWifi: Boolean = false,
    val validityDays: Int = 28,
    val isActive: Boolean = true
)

@Entity(tableName = "academic_events")
data class AcademicEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val feeAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: String = "",
    val semester: String = "Sem 1",
    val isPaid: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double = 0.0,
    val dueDate: String = "",
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false
)

@Entity(tableName = "khata_entries")
data class KhataEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val shopOrFriend: String,
    val itemDescription: String,
    val amount: Double,
    val isSettled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlist_cooldowns")
data class WishlistCooldownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemName: String,
    val price: Double,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val status: String = "COOLING" // COOLING, SKIPPED_SAVED, BOUGHT
)

@Entity(tableName = "cashback_rewards")
data class CashbackRewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceApp: String,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_per_use_items")
data class PricePerUseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemName: String,
    val purchasePrice: Double,
    val usageCount: Int = 1,
    val targetUses: Int = 30,
    val createdAtMillis: Long = System.currentTimeMillis()
)
