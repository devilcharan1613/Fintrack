package com.tracmio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val accountId: Long,
    val type: String,
    val amount: Double,
    val amountPaise: Long = Math.round(amount * 100.0),
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val transferAccountId: Long? = null,
    val transferId: String? = null,
    val subcategory: String = "",
    val personId: Long? = null,
    val paymentMethod: String = "Cash",
    val upiApp: String = ""
)
