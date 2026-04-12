package com.example.bookstore.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val shippingAddress: String,
    val totalAmount: Double,
    val itemsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)