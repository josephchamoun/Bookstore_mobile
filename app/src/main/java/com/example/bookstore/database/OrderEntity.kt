package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders_cache")
data class OrderEntity(
    @PrimaryKey val orderId: Int,
    val orderDate: String,
    val total: Double,
    val status: String,
    val shippingAddress: String,
    val itemsJson: String,
    val cachedAt: String
)