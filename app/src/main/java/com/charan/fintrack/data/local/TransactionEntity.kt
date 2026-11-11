package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions", indices = [Index(value = ["stableUid"], unique = true)])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: String,
    /** Legacy-compatible rupee projection. Exact calculations use amountPaise. */
    val amount: Double,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val transferAccountId: Long? = null,
    val transferId: String? = null,
    val subcategory: String = "",
    val personId: Long? = null,
    @ColumnInfo(defaultValue = "''") val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val amountPaise: Long = 0L,
    @ColumnInfo(defaultValue = "''") val paymentMethod: String = "",
    @ColumnInfo(defaultValue = "''") val upiApp: String = ""
)
