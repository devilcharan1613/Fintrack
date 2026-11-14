package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_payments")
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val loanId: Long,
    val amount: Double = 0.0,
    val amountPaise: Long = Math.round(amount * 100.0),
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
