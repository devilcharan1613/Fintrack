package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "persons", indices = [Index(value = ["stableUid"], unique = true)])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "''") val stableUid: String = UUID.randomUUID().toString()
)
