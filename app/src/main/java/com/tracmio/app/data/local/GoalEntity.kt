package com.tracmio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val targetAmountPaise: Long = Math.round(targetAmount * 100.0),
    val savedAmount: Double = 0.0,
    val savedAmountPaise: Long = Math.round(savedAmount * 100.0),
    val deadline: Long? = null
)
