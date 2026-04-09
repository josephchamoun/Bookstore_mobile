package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart")
data class CartEntity(
    @PrimaryKey val bookId: Int,
    val title: String,
    val author: String,
    val unitPrice: Double,
    val quantity: Int,
    val coverUrl: String?
)