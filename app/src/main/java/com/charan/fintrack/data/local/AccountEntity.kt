package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts", indices = [Index(value = ["stableUid"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountType: String,
    /** Legacy-compatible rupee projection. Exact calculations use paise columns. */
    val balance: Double = 0.0,
    val currency: String = "INR",
    val institution: String = "",
    val accountNumber: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0.0") val creditLimit: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val creditLimitPaise: Long = 0L,
    @ColumnInfo(defaultValue = "''") val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val openingBalancePaise: Long = 0L
)
