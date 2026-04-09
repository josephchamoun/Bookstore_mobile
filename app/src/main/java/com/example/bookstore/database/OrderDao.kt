package com.example.bookstore.database

import androidx.room.*

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders_cache ORDER BY orderDate DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Query("DELETE FROM orders_cache")
    suspend fun clearAll()
}