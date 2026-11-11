package com.charan.tracmio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val parentId: Long? = null,
    val icon: String = "",
    val isSystem: Boolean = false
)
