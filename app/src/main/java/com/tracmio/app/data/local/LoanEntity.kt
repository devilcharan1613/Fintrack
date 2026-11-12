package com.tracmio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val principal: Double,
    val principalPaise: Long = Math.round(principal * 100.0),
    val paidAmount: Double = 0.0,
    val paidAmountPaise: Long = Math.round(paidAmount * 100.0),
    val monthlyPayment: Double = 0.0,
    val monthlyPaymentPaise: Long = Math.round(monthlyPayment * 100.0),
    val interestRate: Double = 0.0,
    val dueDay: Int = 5
)
