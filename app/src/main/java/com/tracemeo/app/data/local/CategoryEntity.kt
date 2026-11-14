package com.tracmio.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["type", "name"]),
        Index(value = ["parentId"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableUid: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val parentId: Long? = null,
    val icon: String = "",
    val isSystem: Boolean = false
)
