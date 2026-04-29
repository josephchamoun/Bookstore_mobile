package com.example.bookstore.repository

import com.example.bookstore.model.Book

// Cart stays local — stored in memory, no Room, no Firestore
// Simple in-memory cart that survives the session
object CartRepository {

    data class CartItem(
        val bookId: String,
        val title: String,
        val author: String,
        val unitPrice: Double,
        var quantity: Int,
        val coverUrl: String
    )

    private val items = mutableListOf<CartItem>()

    fun getCartItems(): List<CartItem> = items.toList()

    fun addToCart(book: Book, quantity: Int = 1) {
        val existing = items.find { it.bookId == book.bookId }
        if (existing != null) {
            existing.quantity += quantity
        } else {
            items.add(
                CartItem(
                    bookId    = book.bookId,
                    title     = book.title,
                    author    = book.author,
                    unitPrice = book.price,
                    quantity  = quantity,
                    coverUrl  = book.coverUrl
                )
            )
        }
    }

    fun updateQuantity(bookId: String, quantity: Int) {
        if (quantity <= 0) items.removeAll { it.bookId == bookId }
        else items.find { it.bookId == bookId }?.quantity = quantity
    }

    fun removeItem(bookId: String) = items.removeAll { it.bookId == bookId }

    fun clearCart() = items.clear()

    fun getCartCount(): Int = items.sumOf { it.quantity }

    fun calculateTotal(): Double = items.sumOf { it.unitPrice * it.quantity }
}