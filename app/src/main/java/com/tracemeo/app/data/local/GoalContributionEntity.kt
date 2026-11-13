package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goal_contributions")
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val goalId: Long,
    val accountId: Long? = null,
    val transactionId: Long? = null,
    val amount: Double,
    val amountPaise: Long = Math.round(amount * 100.0),
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
