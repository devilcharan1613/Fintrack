package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "loans", indices = [Index(value = ["stableUid"], unique = true)])
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Legacy rupee columns retained for compatibility and migration audit. */
    val principal: Double,
    val paidAmount: Double = 0.0,
    val monthlyPayment: Double = 0.0,
    val interestRate: Double = 0.0,
    @ColumnInfo(defaultValue = "''")
    val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val principalPaise: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val paidAmountPaise: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val monthlyPaymentPaise: Long = 0L
)
