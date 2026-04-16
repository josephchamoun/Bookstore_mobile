package com.example.bookstore.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders_cache ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("DELETE FROM orders_cache WHERE orderId NOT IN (:ids)")
    suspend fun deleteAbsent(ids: List<Int>)

    @Query("UPDATE orders_cache SET status = :status WHERE orderId = :orderId")
    suspend fun updateStatus(orderId: Int, status: String)

    @Query("DELETE FROM orders_cache")
    suspend fun clearAll()
}
