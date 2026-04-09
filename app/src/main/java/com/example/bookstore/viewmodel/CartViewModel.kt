package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.database.CartEntity
import com.example.bookstore.model.Book
import com.example.bookstore.repository.CartRepository
import kotlinx.coroutines.launch

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CartRepository(application)

    private val _cartItems  = MutableLiveData<List<CartEntity>>()
    val cartItems: LiveData<List<CartEntity>> = _cartItems

    private val _cartTotal  = MutableLiveData<Double>()
    val cartTotal: LiveData<Double> = _cartTotal

    private val _cartCount  = MutableLiveData<Int>()
    val cartCount: LiveData<Int> = _cartCount

    private val _message    = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun loadCart() {
        viewModelScope.launch {
            val items = repository.getCartItems()
            _cartItems.value  = items
            _cartTotal.value  = repository.calculateTotal(items)
            _cartCount.value  = items.sumOf { it.quantity }
        }
    }

    fun addToCart(book: Book) {
        viewModelScope.launch {
            if (book.stock <= 0) {
                _message.value = "This book is out of stock"
                return@launch
            }
            repository.addToCart(book)
            _message.value = "${book.title} added to cart!"
            loadCart()
        }
    }

    fun updateQuantity(item: CartEntity, quantity: Int) {
        viewModelScope.launch {
            repository.updateQuantity(item, quantity)
            loadCart()
        }
    }

    fun removeItem(item: CartEntity) {
        viewModelScope.launch {
            repository.removeItem(item)
            loadCart()
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            loadCart()
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}