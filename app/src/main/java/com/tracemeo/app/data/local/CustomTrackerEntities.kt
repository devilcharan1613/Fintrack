package com.tracemeo.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_trackers")
data class CustomTrackerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val trackerType: String = "SAVINGS", // SAVINGS, HABIT, COUNTER, MILESTONE, EXPENSE_LIMIT
    val targetValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val unit: String = "₹",
    val icon: String = "🎯",
    val colorHex: String = "#00F5D4",
    val folderId: Long = 0L,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracker_entries")
data class CustomTrackerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackerId: Long,
    val deltaValue: Double,
    val note: String = "",
    val attachmentPath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tracker_folders")
data class TrackerFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val folderName: String,
    val icon: String = "📂",
    val colorHex: String = "#00F5D4",
    val sortOrder: Int = 0
)
