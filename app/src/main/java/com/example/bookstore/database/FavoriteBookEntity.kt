package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_books",
    primaryKeys = ["userId", "bookId"]
)
data class FavoriteBookEntity(
    val userId: Int,
    val bookId: Int,
    val savedAt: Long = System.currentTimeMillis()
)
