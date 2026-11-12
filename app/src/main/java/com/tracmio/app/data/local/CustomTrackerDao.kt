package com.tracmio.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTrackerDao {

    @Query("SELECT * FROM custom_trackers WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveTrackers(): Flow<List<CustomTrackerEntity>>

    @Query("SELECT * FROM custom_trackers WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedTrackers(): Flow<List<CustomTrackerEntity>>

    @Query("SELECT * FROM custom_trackers ORDER BY updatedAt DESC")
    suspend fun getAllTrackersSync(): List<CustomTrackerEntity>

    @Query("SELECT * FROM custom_trackers WHERE stableUid = :trackerUid LIMIT 1")
    suspend fun getTrackerByUid(trackerUid: String): CustomTrackerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracker(tracker: CustomTrackerEntity): Long

    @Update
    suspend fun updateTracker(tracker: CustomTrackerEntity)

    @Query("UPDATE custom_trackers SET isArchived = :archived, status = :status, updatedAt = :updatedAt WHERE stableUid = :trackerUid")
    suspend fun setTrackerArchived(trackerUid: String, archived: Boolean, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM custom_trackers WHERE stableUid = :trackerUid")
    suspend fun deleteTrackerByUid(trackerUid: String)

    // Many-to-many Tracker <-> Transaction links
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTransactionToTracker(crossRef: TrackerTransactionCrossRefEntity): Long

    @Query("DELETE FROM tracker_transactions WHERE trackerUid = :trackerUid AND transactionUid = :transactionUid")
    suspend fun unlinkTransactionFromTracker(trackerUid: String, transactionUid: String)

    @Query("SELECT * FROM tracker_transactions WHERE trackerUid = :trackerUid ORDER BY linkedAt DESC")
    suspend fun getLinksForTracker(trackerUid: String): List<TrackerTransactionCrossRefEntity>

    @Query("SELECT * FROM tracker_transactions")
    suspend fun getAllTrackerLinks(): List<TrackerTransactionCrossRefEntity>

    // Budgets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackerBudget(budget: TrackerBudgetEntity)

    @Query("SELECT * FROM tracker_budgets WHERE trackerUid = :trackerUid LIMIT 1")
    suspend fun getTrackerBudget(trackerUid: String): TrackerBudgetEntity?

    // People & Settlements
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackerPerson(person: TrackerPersonEntity)

    @Query("SELECT * FROM tracker_people WHERE trackerUid = :trackerUid")
    suspend fun getTrackerPeople(trackerUid: String): List<TrackerPersonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackerSettlement(settlement: TrackerSettlementEntity)

    @Query("SELECT * FROM tracker_settlements WHERE trackerUid = :trackerUid ORDER BY timestamp DESC")
    suspend fun getTrackerSettlements(trackerUid: String): List<TrackerSettlementEntity>

    // Attachments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackerAttachment(attachment: TrackerAttachmentEntity)

    @Query("DELETE FROM tracker_attachments WHERE attachmentUid = :attachmentUid")
    suspend fun deleteTrackerAttachment(attachmentUid: String)

    @Query("SELECT * FROM tracker_attachments WHERE trackerUid = :trackerUid ORDER BY addedAt DESC")
    suspend fun getTrackerAttachments(trackerUid: String): List<TrackerAttachmentEntity>

    // Custom Fields & Values
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomField(field: TrackerCustomFieldEntity)

    @Query("DELETE FROM tracker_custom_fields WHERE fieldUid = :fieldUid")
    suspend fun deleteCustomField(fieldUid: String)

    @Query("SELECT * FROM tracker_custom_fields WHERE trackerUid = :trackerUid")
    suspend fun getCustomFields(trackerUid: String): List<TrackerCustomFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomValue(value: TrackerCustomValueEntity)

    @Query("SELECT * FROM tracker_custom_values WHERE trackerUid = :trackerUid")
    suspend fun getCustomValues(trackerUid: String): List<TrackerCustomValueEntity>

    // Recurring Rules & Alert Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurringRule(rule: TrackerRecurringRuleEntity)

    @Query("SELECT * FROM tracker_recurring_rules WHERE trackerUid = :trackerUid LIMIT 1")
    suspend fun getRecurringRule(trackerUid: String): TrackerRecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlertSetting(setting: TrackerAlertSettingEntity)

    @Query("SELECT * FROM tracker_alert_settings WHERE trackerUid = :trackerUid LIMIT 1")
    suspend fun getAlertSetting(trackerUid: String): TrackerAlertSettingEntity?

    // Activity Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: TrackerActivityLogEntity)

    @Query("SELECT * FROM tracker_activity_logs WHERE trackerUid = :trackerUid ORDER BY timestamp DESC")
    suspend fun getActivityLogs(trackerUid: String): List<TrackerActivityLogEntity>

    @Transaction
    suspend fun replaceAllTrackersFromSnapshot(
        trackers: List<CustomTrackerEntity>,
        links: List<TrackerTransactionCrossRefEntity>,
        people: List<TrackerPersonEntity>,
        settlements: List<TrackerSettlementEntity>,
        attachments: List<TrackerAttachmentEntity>,
        customFields: List<TrackerCustomFieldEntity>,
        customValues: List<TrackerCustomValueEntity>,
        logs: List<TrackerActivityLogEntity>
    ) {
        trackers.forEach { upsertTracker(it) }
        links.forEach { linkTransactionToTracker(it) }
        people.forEach { upsertTrackerPerson(it) }
        settlements.forEach { upsertTrackerSettlement(it) }
        attachments.forEach { upsertTrackerAttachment(it) }
        customFields.forEach { upsertCustomField(it) }
        customValues.forEach { upsertCustomValue(it) }
        logs.forEach { insertActivityLog(it) }
    }
}
