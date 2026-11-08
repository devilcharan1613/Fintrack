package com.charan.fintrack.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories", indices = [Index(value = ["stableUid"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val parentId: Long? = null,
    val icon: String = "",
    val isSystem: Boolean = false,
    @ColumnInfo(defaultValue = "''") val stableUid: String = UUID.randomUUID().toString()
)
