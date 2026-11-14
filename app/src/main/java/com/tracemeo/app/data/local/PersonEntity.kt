package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val upiId: String = "",
    val netBalance: Double = 0.0,
    val netBalancePaise: Long = Math.round(netBalance * 100.0),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
