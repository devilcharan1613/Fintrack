package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts", indices = [Index(value = ["stableUid"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountType: String,
    /** Legacy rupee column retained only for v3 compatibility and reconciliation. */
    val balance: Double = 0.0,
    val currency: String = "INR",
    val institution: String = "",
    val accountNumber: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''")
    val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val openingBalancePaise: Long = 0L
)
