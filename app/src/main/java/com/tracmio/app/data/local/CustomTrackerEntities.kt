package com.tracmio.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_trackers",
    indices = [
        Index(value = ["stableUid"], unique = true),
        Index(value = ["status"]),
        Index(value = ["isArchived"])
    ]
)
data class CustomTrackerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stableUid: String,
    val name: String,
    val trackerType: String,
    val customTypeName: String = "",
    val description: String = "",
    val icon: String = "🎯",
    val themeColor: String = "#00f5d4",
    val startDate: String = "",
    val endDate: String = "",
    val budgetAmount: Double = 0.0,
    val budgetMode: String = "NONE", // NONE, FIXED, CUSTOM
    val currency: String = "₹",
    val notes: String = "",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, ARCHIVED
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracker_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackerUid", "transactionUid"], unique = true),
        Index(value = ["transactionUid"])
    ]
)
data class TrackerTransactionCrossRefEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerUid: String,
    val transactionUid: String,
    val isRefund: Boolean = false,
    val paidByPersonUid: String = "",
    val splitMode: String = "EQUAL", // EQUAL, CUSTOM, NONE
    val splitSharesJson: String = "{}",
    val linkedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracker_budgets",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerUid"], unique = true)]
)
data class TrackerBudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerUid: String,
    val budgetMode: String = "FIXED", // NONE, FIXED, CUSTOM
    val plannedAmount: Double = 0.0,
    val adjustmentAmount: Double = 0.0,
    val startDate: String = "",
    val endDate: String = "",
    val categoryLimitsJson: String = "{}"
)

@Entity(
    tableName = "tracker_people",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackerUid"]),
        Index(value = ["personUid"], unique = true)
    ]
)
data class TrackerPersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personUid: String,
    val trackerUid: String,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val avatarColor: String = "#00f5d4"
)

@Entity(
    tableName = "tracker_settlements",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackerUid"]),
        Index(value = ["settlementUid"], unique = true)
    ]
)
data class TrackerSettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val settlementUid: String,
    val trackerUid: String,
    val fromPersonUid: String,
    val toPersonUid: String,
    val amount: Double,
    val isFullSettlement: Boolean = false,
    val date: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracker_attachments",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackerUid"]),
        Index(value = ["attachmentUid"], unique = true)
    ]
)
data class TrackerAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val attachmentUid: String,
    val trackerUid: String,
    val transactionUid: String = "",
    val fileName: String,
    val mimeType: String,
    val localPath: String,
    val contentUri: String = "",
    val fileSizeBytes: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracker_custom_fields",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackerUid"]),
        Index(value = ["fieldUid"], unique = true)
    ]
)
data class TrackerCustomFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fieldUid: String,
    val trackerUid: String,
    val fieldName: String,
    val fieldType: String, // TEXT, NUMBER, DATE, DROPDOWN, CHECKBOX, MULTI_SELECT
    val optionsJson: String = "[]"
)

@Entity(
    tableName = "tracker_custom_values",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["trackerUid", "fieldUid"], unique = true)
    ]
)
data class TrackerCustomValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerUid: String,
    val fieldUid: String,
    val fieldValue: String
)

@Entity(
    tableName = "tracker_recurring_rules",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerUid"], unique = true)]
)
data class TrackerRecurringRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerUid: String,
    val frequency: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    val customIntervalDays: Int = 30,
    val startDate: String = "",
    val endDate: String = "",
    val nextOccurrenceDate: String = "",
    val reminderDaysBefore: Int = 1,
    val status: String = "ACTIVE" // ACTIVE, PAUSED, STOPPED
)

@Entity(
    tableName = "tracker_alert_settings",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerUid"], unique = true)]
)
data class TrackerAlertSettingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackerUid: String,
    val alert50: Boolean = true,
    val alert75: Boolean = true,
    val alert90: Boolean = true,
    val alert100: Boolean = true,
    val customPercent: Int = 0,
    val firedThresholdsJson: String = "[]"
)

@Entity(
    tableName = "tracker_activity_logs",
    foreignKeys = [
        ForeignKey(
            entity = CustomTrackerEntity::class,
            parentColumns = ["stableUid"],
            childColumns = ["trackerUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerUid", "timestamp"])]
)
data class TrackerActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logUid: String,
    val trackerUid: String,
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
