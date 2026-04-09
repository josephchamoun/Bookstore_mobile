package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories_cache")
data class CategoryEntity(
    @PrimaryKey val categoryId: Int,
    val name: String,
    val cachedAt: String
)