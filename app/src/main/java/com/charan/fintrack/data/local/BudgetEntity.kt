package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets", indices = [Index(value = ["stableUid"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    /** Legacy rupee column retained for compatibility and migration audit. */
    val limitAmount: Double,
    val month: String,
    @ColumnInfo(defaultValue = "''")
    val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val limitAmountPaise: Long = 0L
)
