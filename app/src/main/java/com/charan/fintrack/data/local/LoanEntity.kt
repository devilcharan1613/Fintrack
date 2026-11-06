package com.charan.fintrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val principal: Double,
    val paidAmount: Double = 0.0,
    val monthlyPayment: Double = 0.0,
    val interestRate: Double = 0.0
)
