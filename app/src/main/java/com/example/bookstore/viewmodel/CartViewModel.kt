package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.model.Book
import com.example.bookstore.repository.CartRepository
import com.example.bookstore.repository.OrderRepository
import kotlinx.coroutines.launch

sealed class OrderState {
    object Idle    : OrderState()
    object Success : OrderState()
    data class Error(val message: String) : OrderState()
}

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val cartRepository  = CartRepository
    private val orderRepository = OrderRepository()

    private val _cartItems = MutableLiveData<List<CartRepository.CartItem>>()
    val cartItems: LiveData<List<CartRepository.CartItem>> = _cartItems

    private val _cartTotal = MutableLiveData<Double>()
    val cartTotal: LiveData<Double> = _cartTotal

    private val _cartCount = MutableLiveData<Int>()
    val cartCount: LiveData<Int> = _cartCount

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _orderState = MutableLiveData<OrderState>(OrderState.Idle)
    val orderState: LiveData<OrderState> = _orderState

    fun loadCart() {
        val items = cartRepository.getCartItems()
        _cartItems.value  = items
        _cartTotal.value  = cartRepository.calculateTotal()
        _cartCount.value  = cartRepository.getCartCount()
    }

    fun addToCart(book: Book) {
        if (book.stock <= 0) { _message.value = "This book is out of stock"; return }
        cartRepository.addToCart(book)
        _message.value = "${book.title} added to cart!"
        loadCart()
    }

    fun updateQuantity(bookId: String, quantity: Int) {
        cartRepository.updateQuantity(bookId, quantity)
        loadCart()
    }

    fun removeItem(bookId: String) {
        cartRepository.removeItem(bookId)
        loadCart()
    }

    fun clearCart() { cartRepository.clearCart(); loadCart() }

    fun clearMessage() { _message.value = null }

    fun placeOrder(shippingAddress: String) {
        viewModelScope.launch {
            val items = cartRepository.getCartItems()
            if (items.isEmpty()) return@launch
            // Firestore handles offline automatically — no SavedOffline state needed
            val result = orderRepository.placeOrder(items, shippingAddress)
            _orderState.value = if (result.isSuccess) OrderState.Success
            else OrderState.Error(result.exceptionOrNull()?.message ?: "Order failed")
            _orderState.value = OrderState.Idle
        }
    }
}