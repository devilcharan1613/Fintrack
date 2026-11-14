package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val monthlyLimit: Double = 0.0,
    val limitPaise: Long = Math.round(monthlyLimit * 100.0),
    val month: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
