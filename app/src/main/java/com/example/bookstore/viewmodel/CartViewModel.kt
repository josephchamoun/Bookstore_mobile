package com.example.bookstore.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.database.CartEntity
import com.example.bookstore.model.Book
import com.example.bookstore.network.SessionManager
import com.example.bookstore.repository.CartRepository
import com.example.bookstore.repository.OrderRepository
import com.example.bookstore.worker.OrderSyncWorker
import kotlinx.coroutines.launch

sealed class OrderState {
    object Idle : OrderState()
    object Success : OrderState()
    object SavedOffline : OrderState()
    data class Error(val message: String) : OrderState()
}

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val cartRepository = CartRepository(application)
    private val orderRepository = OrderRepository(application, sessionManager)

    private val _cartItems = MutableLiveData<List<CartEntity>>()
    val cartItems: LiveData<List<CartEntity>> = _cartItems

    private val _cartTotal = MutableLiveData<Double>()
    val cartTotal: LiveData<Double> = _cartTotal

    private val _cartCount = MutableLiveData<Int>()
    val cartCount: LiveData<Int> = _cartCount

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _orderState = MutableLiveData<OrderState>(OrderState.Idle)
    val orderState: LiveData<OrderState> = _orderState

    fun loadCart() {
        viewModelScope.launch {
            val items = cartRepository.getCartItems()
            _cartItems.value = items
            _cartTotal.value = cartRepository.calculateTotal(items)
            _cartCount.value = items.sumOf { it.quantity }
        }
    }

    fun addToCart(book: Book) {
        viewModelScope.launch {
            if (book.stock <= 0) {
                _message.value = "This book is out of stock"
                return@launch
            }
            cartRepository.addToCart(book)
            _message.value = "${book.title} added to cart!"
            loadCart()
        }
    }

    fun updateQuantity(item: CartEntity, quantity: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(item, quantity)
            loadCart()
        }
    }

    fun removeItem(item: CartEntity) {
        viewModelScope.launch {
            cartRepository.removeItem(item)
            loadCart()
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
            loadCart()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun placeOrder(shippingAddress: String) {
        viewModelScope.launch {
            Log.d("CartVM", "placeOrder called, address: $shippingAddress")
            val items = cartRepository.getCartItems()
            Log.d("CartVM", "cart items count: ${items.size}")
            if (items.isEmpty()) return@launch

            val total = cartRepository.calculateTotal(items)
            val result = orderRepository.placeOrder(items, shippingAddress)

            when {
                result.isSuccess -> {
                    sessionManager.saveAddress(shippingAddress)
                    cartRepository.clearCart()
                    loadCart()
                    _orderState.value = OrderState.Success
                }
                result.exceptionOrNull() is java.io.IOException -> {
                    Log.d("CartVM", "server unreachable, saving offline")
                    orderRepository.saveOffline(items, shippingAddress, total)
                    sessionManager.saveAddress(shippingAddress)
                    cartRepository.clearCart()
                    loadCart()
                    OrderSyncWorker.scheduleNow(getApplication())
                    _orderState.value = OrderState.SavedOffline
                }
                else -> {
                    _orderState.value = OrderState.Error(
                        result.exceptionOrNull()?.message ?: "Order failed"
                    )
                }
            }

            _orderState.value = OrderState.Idle
        }
    }

    fun getSavedAddress(): String = sessionManager.getAddress().orEmpty()
}
