package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val accountType: String = "Bank",
    val type: String = accountType,
    val balance: Double = 0.0,
    val balancePaise: Long = Math.round(balance * 100.0),
    val creditLimit: Double = 0.0,
    val creditLimitPaise: Long = Math.round(creditLimit * 100.0),
    val statementDay: Int = 1,
    val dueDay: Int = 15,
    val allocationsJson: String = "[]",
    val accountNumber: String = "",
    val isArchived: Boolean = false,
    val colorHex: String = "#00F5D4",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
