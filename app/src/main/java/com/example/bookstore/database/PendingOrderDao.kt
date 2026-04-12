package com.example.bookstore.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: PendingOrderEntity): Long

    @Query("SELECT * FROM pending_orders ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PendingOrderEntity>>

    @Delete
    suspend fun delete(order: PendingOrderEntity)

    @Query("UPDATE pending_orders SET retryCount = retryCount + 1 WHERE localId = :id")
    suspend fun incrementRetry(id: Int)
}