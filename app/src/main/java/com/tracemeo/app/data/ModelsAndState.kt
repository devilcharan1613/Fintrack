package com.tracemeo.app.data

import com.tracemeo.app.data.local.*

data class AccountAllocation(
    val id: String = "1",
    val category: String,
    val allocatedAmount: Double = 0.0,
    val percentage: Double = 0.0
) {
    constructor(category: String, percentage: Double) : this("1", category, 0.0, percentage)
}

data class QuickSpendChip(
    val id: String,
    val emoji: String,
    val title: String,
    val amount: Double,
    val category: String = "Food & Chai"
)

data class PendingRefundItem(
    val id: String = "",
    val title: String,
    val source: String = "Store / App",
    val amount: Double,
    val expectedDate: String = "Soon",
    val isReceived: Boolean = false
)

enum class SpendingChartWindow {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

data class UpiSmsDraft(
    val merchant: String,
    val amount: Double,
    val type: String,
    val upiApp: String,
    val paymentMethod: String = "UPI"
)

data class AppLockState(
    val isEnabled: Boolean = false,
    val pin: String = "",
    val pinLength: Int = 4,
    val biometricEnabled: Boolean = false,
    val securityQuestion: String = "",
    val securityAnswerHash: String = "",
    val recoveryKey: String = ""
)

data class NativeTracmioUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val goalContributions: List<GoalContributionEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val loanPayments: List<LoanPaymentEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val customTrackers: List<CustomTrackerEntity> = emptyList(),
    val trackerEntries: List<CustomTrackerEntryEntity> = emptyList(),
    val trackerFolders: List<TrackerFolderEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val academicEvents: List<AcademicEventEntity> = emptyList(),
    val reminders: List<ReminderItemEntity> = emptyList(),
    val khataEntries: List<KhataEntryEntity> = emptyList(),
    val wishlistCooldowns: List<WishlistCooldownEntity> = emptyList(),
    val cashbackRewards: List<CashbackRewardEntity> = emptyList(),
    val pricePerUseItems: List<PricePerUseEntity> = emptyList(),
    val pendingRefunds: List<PendingRefundItem> = emptyList(),
    val quickSpendChips: List<QuickSpendChip> = listOf(
        QuickSpendChip("q1", "☕", "Tea", 20.0, "Food & Chai"),
        QuickSpendChip("q2", "🚌", "Auto", 50.0, "Transport & Travel"),
        QuickSpendChip("q3", "🍽️", "Lunch", 120.0, "Food & Chai"),
        QuickSpendChip("q4", "📱", "Recharge", 199.0, "SIM, WiFi & Bills")
    ),
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val statusMessage: String? = null,
    val recentlyDeletedTransaction: TransactionEntity? = null,
    val recentlyDeletedChip: QuickSpendChip? = null,
    val hasPendingUndo: Boolean = false,
    val isAppLocked: Boolean = false,
    val appLockState: AppLockState = AppLockState(),
    val categories: List<String> = listOf(
        "Food & Chai",
        "Khata & Kirana",
        "Transport & Travel",
        "SIM, WiFi & Bills",
        "College & Exams",
        "Family & Income",
        "Shopping & Personal",
        "Health & Medical",
        "Entertainment",
        "Investment",
        "Fuel & Commute",
        "Rent & Housing",
        "Recharge & WiFi",
        "Gifts & Celebrations",
        "Business & Studio",
        "Pets & Care",
        "Other"
    ),
    val pendingNotificationAction: String? = null,
    val pinnedToolIds: List<String> = emptyList(),
    val dailyReminderEnabled: Boolean = false
)
