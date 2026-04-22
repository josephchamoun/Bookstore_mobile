package com.example.bookstore.model

import com.google.gson.annotations.SerializedName


data class AuthResponse(
    val message: String?,
    val token: String?,
    val user: User?,
    val error: String?
)

data class BooksResponse(
    val books: List<Book>?,
    val error: String?,
    @SerializedName("last_updated") val lastUpdated: String? = null  // ADD THIS
)

data class BookResponse(
    val book: Book?,
    val error: String?
)

data class CategoriesResponse(
    val categories: List<Category>?,
    val error: String?
)

data class OrdersResponse(
    val orders: List<Order>?,
    val error: String?,
    @SerializedName("last_updated") val lastUpdated: String? = null
)

data class PlaceOrderResponse(
    val message: String?,
    val order_id: Int?,
    val total: Double?,
    val error: String?
)

data class MessageResponse(
    val message: String?,
    val error: String?
)