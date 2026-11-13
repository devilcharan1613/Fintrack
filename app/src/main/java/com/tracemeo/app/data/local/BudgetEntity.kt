package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val category: String,
    val limitAmount: Double,
    val limitAmountPaise: Long = Math.round(limitAmount * 100.0),
    val month: String
)
