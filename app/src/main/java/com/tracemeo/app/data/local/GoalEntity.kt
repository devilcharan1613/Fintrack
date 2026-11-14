package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String = "",
    val title: String = name,
    val targetAmount: Double = 0.0,
    val targetPaise: Long = Math.round(targetAmount * 100.0),
    val currentAmount: Double = 0.0,
    val currentPaise: Long = Math.round(currentAmount * 100.0),
    val deadline: String = "",
    val icon: String = "🎯",
    val colorHex: String = "#00F5D4",
    val createdAt: Long = System.currentTimeMillis()
)
