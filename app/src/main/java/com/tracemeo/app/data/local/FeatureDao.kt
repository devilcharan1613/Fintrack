package com.tracemeo.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeatureDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextDueDateMillis ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(item: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(item: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(item: SubscriptionEntity)

    @Query("SELECT * FROM academic_events ORDER BY id DESC")
    fun getAllAcademicEvents(): Flow<List<AcademicEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicEvent(item: AcademicEventEntity): Long

    @Update
    suspend fun updateAcademicEvent(item: AcademicEventEntity)

    @Delete
    suspend fun deleteAcademicEvent(item: AcademicEventEntity)

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, id DESC")
    fun getAllReminders(): Flow<List<ReminderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(item: ReminderItemEntity): Long

    @Update
    suspend fun updateReminder(item: ReminderItemEntity)

    @Delete
    suspend fun deleteReminder(item: ReminderItemEntity)

    @Query("SELECT * FROM khata_entries ORDER BY isSettled ASC, timestamp DESC")
    fun getAllKhataEntries(): Flow<List<KhataEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(item: KhataEntryEntity): Long

    @Update
    suspend fun updateKhataEntry(item: KhataEntryEntity)

    @Delete
    suspend fun deleteKhataEntry(item: KhataEntryEntity)

    @Query("SELECT * FROM wishlist_cooldowns ORDER BY createdAtMillis DESC")
    fun getAllWishlistCooldowns(): Flow<List<WishlistCooldownEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistCooldown(item: WishlistCooldownEntity): Long

    @Update
    suspend fun updateWishlistCooldown(item: WishlistCooldownEntity)

    @Delete
    suspend fun deleteWishlistCooldown(item: WishlistCooldownEntity)

    @Query("SELECT * FROM cashback_rewards ORDER BY timestamp DESC")
    fun getAllCashbackRewards(): Flow<List<CashbackRewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashbackReward(item: CashbackRewardEntity): Long

    @Delete
    suspend fun deleteCashbackReward(item: CashbackRewardEntity)

    @Query("SELECT * FROM price_per_use_items ORDER BY createdAtMillis DESC")
    fun getAllPricePerUseItems(): Flow<List<PricePerUseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricePerUseItem(item: PricePerUseEntity): Long

    @Update
    suspend fun updatePricePerUseItem(item: PricePerUseEntity)

    @Delete
    suspend fun deletePricePerUseItem(item: PricePerUseEntity)
}
