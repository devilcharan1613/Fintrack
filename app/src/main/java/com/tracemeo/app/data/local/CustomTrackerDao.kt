package com.tracemeo.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTrackerDao {
    @Query("SELECT * FROM custom_trackers ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllTrackers(): Flow<List<CustomTrackerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: CustomTrackerEntity): Long

    @Update
    suspend fun updateTracker(tracker: CustomTrackerEntity)

    @Delete
    suspend fun deleteTracker(tracker: CustomTrackerEntity)

    @Query("SELECT * FROM tracker_entries WHERE trackerId = :trackerId ORDER BY timestamp DESC")
    fun getEntriesForTracker(trackerId: Long): Flow<List<CustomTrackerEntryEntity>>

    @Query("SELECT * FROM tracker_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<CustomTrackerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CustomTrackerEntryEntity): Long

    @Delete
    suspend fun deleteEntry(entry: CustomTrackerEntryEntity)

    @Query("SELECT * FROM tracker_folders ORDER BY sortOrder ASC, id ASC")
    fun getAllFolders(): Flow<List<TrackerFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: TrackerFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: TrackerFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: TrackerFolderEntity)
}
