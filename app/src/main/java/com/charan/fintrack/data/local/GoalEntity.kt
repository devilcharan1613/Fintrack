package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goals", indices = [Index(value = ["stableUid"], unique = true)])
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Legacy rupee columns retained for compatibility and migration audit. */
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val deadline: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val stableUid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val targetAmountPaise: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val savedAmountPaise: Long = 0L
)
