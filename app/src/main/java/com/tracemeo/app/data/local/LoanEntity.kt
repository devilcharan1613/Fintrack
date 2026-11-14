package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val lender: String = name,
    val principal: Double = 0.0,
    val principalPaise: Long = Math.round(principal * 100.0),
    val interestRate: Double = 0.0,
    val tenureMonths: Int = 12,
    val monthlyPayment: Double = 0.0,
    val emiPaise: Long = Math.round(monthlyPayment * 100.0),
    val startDateMillis: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false
)
