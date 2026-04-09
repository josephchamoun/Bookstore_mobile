package com.example.bookstore.database

import androidx.room.*

@Dao
interface CartDao {

    @Query("SELECT * FROM cart")
    suspend fun getAllItems(): List<CartEntity>

    @Query("SELECT * FROM cart WHERE bookId = :bookId")
    suspend fun getItem(bookId: Int): CartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CartEntity)

    @Update
    suspend fun updateItem(item: CartEntity)

    @Delete
    suspend fun deleteItem(item: CartEntity)

    @Query("DELETE FROM cart")
    suspend fun clearCart()

    @Query("SELECT COUNT(*) FROM cart")
    suspend fun getCartCount(): Int
}