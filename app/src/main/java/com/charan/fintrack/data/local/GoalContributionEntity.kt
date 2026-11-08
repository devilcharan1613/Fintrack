package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Savings contribution history is tracking-only and never mutates an account ledger. */
@Entity(
    tableName = "goal_contributions",
    indices = [Index(value = ["stableUid"], unique = true), Index(value = ["goalId", "timestamp"])]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    /** Legacy-compatible rupee projection. */
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    @ColumnInfo(defaultValue = "''") val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val amountPaise: Long = 0L
)
