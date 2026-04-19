package com.example.bookstore.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: PendingReviewEntity)

    // Flow so ReviewSyncWorker reacts to new pending reviews automatically
    @Query("SELECT * FROM pending_reviews ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PendingReviewEntity>>

    @Query("DELETE FROM pending_reviews WHERE localId = :localId")
    suspend fun delete(localId: Int)

    @Query("UPDATE pending_reviews SET retryCount = retryCount + 1 WHERE localId = :localId")
    suspend fun incrementRetry(localId: Int)
}