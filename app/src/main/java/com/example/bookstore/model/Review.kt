package com.example.bookstore.model

data class Review(
    val reviewId: String = "",
    val bookId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: com.google.firebase.Timestamp? = null
)