package com.example.bookstore.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteBookDao {

    @Query("SELECT bookId FROM favorite_books WHERE userId = :userId")
    fun observeFavoriteIds(userId: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteBookEntity)

    @Query("DELETE FROM favorite_books WHERE userId = :userId AND bookId = :bookId")
    suspend fun delete(userId: Int, bookId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_books WHERE userId = :userId AND bookId = :bookId)")
    suspend fun isFavorite(userId: Int, bookId: Int): Boolean
}
