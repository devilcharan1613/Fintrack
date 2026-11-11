package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "loan_payments", indices = [Index(value = ["stableUid"], unique = true)])
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    /** Legacy rupee amount retained for compatibility and migration audit. */
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    @ColumnInfo(defaultValue = "''")
    val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val amountPaise: Long = 0L
)
