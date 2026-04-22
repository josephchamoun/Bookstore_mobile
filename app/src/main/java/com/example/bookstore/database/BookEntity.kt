package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books_cache")
data class BookEntity(
    @PrimaryKey val bookId: Int,
    val categoryId: Int,
    val title: String,
    val author: String,
    val price: Double,
    val stock: Int,
    val coverUrl: String?,
    val categoryName: String?,
    val cachedAt: String,
    val hasEbook: Boolean = false,
    val updatedAt: String? = null
)