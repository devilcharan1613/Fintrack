package com.tracmio.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeatureDao {
    @Query("SELECT * FROM quick_templates ORDER BY id ASC")
    fun quickTemplates(): Flow<List<QuickTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickTemplate(item: QuickTemplateEntity): Long

    @Delete
    suspend fun deleteQuickTemplate(item: QuickTemplateEntity)

    @Query("DELETE FROM quick_templates WHERE id = :id")
    suspend fun deleteQuickTemplateById(id: Long)

    @Query("DELETE FROM quick_templates")
    suspend fun deleteAllQuickTemplates()

    @Query("SELECT * FROM cooling_wishlist ORDER BY addedAt DESC")
    fun wishlistItems(): Flow<List<WishlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItemEntity): Long

    @Update
    suspend fun updateWishlistItem(item: WishlistItemEntity)

    @Delete
    suspend fun deleteWishlistItem(item: WishlistItemEntity)

    @Query("DELETE FROM cooling_wishlist")
    suspend fun deleteAllWishlistItems()

    @Query("SELECT * FROM cashback_entries ORDER BY timestamp DESC")
    fun cashbackEntries(): Flow<List<CashbackEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashbackEntry(item: CashbackEntryEntity): Long

    @Delete
    suspend fun deleteCashbackEntry(item: CashbackEntryEntity)

    @Query("DELETE FROM cashback_entries")
    suspend fun deleteAllCashbackEntries()

    @Query("SELECT * FROM value_items ORDER BY createdAt DESC")
    fun valueItems(): Flow<List<ValueItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValueItem(item: ValueItemEntity): Long

    @Update
    suspend fun updateValueItem(item: ValueItemEntity)

    @Delete
    suspend fun deleteValueItem(item: ValueItemEntity)

    @Query("DELETE FROM value_items")
    suspend fun deleteAllValueItems()

    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC, id ASC")
    fun reminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC, id ASC")
    suspend fun getAllRemindersSync(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(item: ReminderEntity): Long

    @Update
    suspend fun updateReminder(item: ReminderEntity)

    @Delete
    suspend fun deleteReminder(item: ReminderEntity)

    @Query("DELETE FROM reminders WHERE stableUid = :stableUid")
    suspend fun deleteReminderByUid(stableUid: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsSync(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppSettings(settings: AppSettingsEntity)
}
