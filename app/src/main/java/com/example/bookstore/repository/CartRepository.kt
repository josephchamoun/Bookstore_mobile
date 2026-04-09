package com.example.bookstore.repository

import android.content.Context
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.database.CartEntity
import com.example.bookstore.model.Book

class CartRepository(context: Context) {

    private val cartDao = AppDatabase.getInstance(context).cartDao()

    suspend fun getCartItems(): List<CartEntity> = cartDao.getAllItems()

    suspend fun addToCart(book: Book, quantity: Int = 1) {
        val existing = cartDao.getItem(book.bookId)
        if (existing != null) {
            cartDao.updateItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            cartDao.insertItem(
                CartEntity(
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

    suspend fun updateQuantity(item: CartEntity, quantity: Int) {
        if (quantity <= 0) cartDao.deleteItem(item)
        else cartDao.updateItem(item.copy(quantity = quantity))
    }

    suspend fun removeItem(item: CartEntity) = cartDao.deleteItem(item)

    suspend fun clearCart() = cartDao.clearCart()

    suspend fun getCartCount(): Int = cartDao.getCartCount()

    fun calculateTotal(items: List<CartEntity>): Double =
        items.sumOf { it.unitPrice * it.quantity }
}