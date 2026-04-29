package com.example.bookstore.model

data class Book(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val coverUrl: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val hasEbook: Boolean = false,
    val ebookUrl: String = "",
    val isFavorite: Boolean = false
)