package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_reviews")
data class PendingReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val bookId: Int,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)