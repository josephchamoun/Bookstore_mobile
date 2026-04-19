package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews_cache")
data class ReviewEntity(
    @PrimaryKey
    val reviewId: Int,
    val bookId: Int,
    val userId: Int,
    val userName: String,
    val rating: Int,
    val comment: String,
    val status: String,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)