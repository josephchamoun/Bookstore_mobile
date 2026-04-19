package com.example.bookstore.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    // ── Reactive stream — never suspend for list reads ────────────────────────
    @Query("SELECT * FROM reviews_cache WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getReviewsByBook(bookId: Int): Flow<List<ReviewEntity>>

    @Upsert
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    // Delete reviews for a book that are no longer in the latest API response
    @Query("""
        DELETE FROM reviews_cache 
        WHERE bookId = :bookId 
        AND reviewId NOT IN (:ids)
    """)
    suspend fun deleteAbsentForBook(bookId: Int, ids: List<Int>)

    // Called when user navigates away — full cache clear for a book
    @Query("DELETE FROM reviews_cache WHERE bookId = :bookId")
    suspend fun clearByBook(bookId: Int)
}