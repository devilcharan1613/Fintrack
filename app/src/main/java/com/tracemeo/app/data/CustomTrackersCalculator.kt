package com.tracemeo.app.data

import com.tracemeo.app.data.local.CustomTrackerEntity
import com.tracemeo.app.data.local.CustomTrackerEntryEntity
import com.tracemeo.app.data.local.TrackerFolderEntity

object CustomTrackersCalculator {
    data class TrackerSummary(
        val totalTrackers: Int,
        val completedCount: Int,
        val completionRatio: Float,
        val totalSavedValue: Double
    )

    fun computeProgress(tracker: CustomTrackerEntity): Float {
        if (tracker.targetValue <= 0.0) return 0f
        return (tracker.currentValue / tracker.targetValue).toFloat().coerceIn(0f, 1f)
    }

    fun summarizeTrackers(trackers: List<CustomTrackerEntity>): TrackerSummary {
        val completed = trackers.count { it.targetValue > 0.0 && it.currentValue >= it.targetValue }
        val ratio = if (trackers.isEmpty()) 0f else completed.toFloat() / trackers.size.toFloat()
        val totalSaved = trackers.filter { it.trackerType.equals("SAVINGS", true) }.sumOf { it.currentValue }
        return TrackerSummary(
            totalTrackers = trackers.size,
            completedCount = completed,
            completionRatio = ratio,
            totalSavedValue = totalSaved
        )
    }

    fun groupTrackersByFolder(
        trackers: List<CustomTrackerEntity>,
        folders: List<TrackerFolderEntity>
    ): Map<String, List<CustomTrackerEntity>> {
        val folderNames = folders.associateBy({ it.id }, { it.folderName })
        return trackers.groupBy { tracker ->
            folderNames[tracker.folderId] ?: "All Trackers"
        }
    }

    fun applyEntryDelta(
        tracker: CustomTrackerEntity,
        entry: CustomTrackerEntryEntity
    ): CustomTrackerEntity {
        val nextVal = (tracker.currentValue + entry.deltaValue).coerceAtLeast(0.0)
        return tracker.copy(currentValue = nextVal, updatedAt = entry.timestamp)
    }
}
