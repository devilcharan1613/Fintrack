package com.charan.fintrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val parentId: Long? = null,
    val icon: String = "",
    val isSystem: Boolean = false
)
