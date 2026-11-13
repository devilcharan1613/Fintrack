package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isArchived"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val accountType: String,
    val balance: Double = 0.0,
    val balancePaise: Long = Math.round(balance * 100.0),
    val currency: String = "INR",
    val institution: String = "",
    val accountNumber: String = "",
    val notes: String = "",
    val allocationsJson: String = "[]",
    val creditLimit: Double = 0.0,
    val billingCycleDay: Int = 1,
    val dueDay: Int = 15,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
