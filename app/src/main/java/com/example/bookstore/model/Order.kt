package com.example.bookstore.model

data class OrderItem(
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val coverUrl: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val orderDate: com.google.firebase.Timestamp? = null,
    val total: Double = 0.0,
    val status: String = "pending",
    val shippingAddress: String = "",
    val items: List<OrderItem> = emptyList()
)