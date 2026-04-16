package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.database.CartEntity
import com.example.bookstore.model.Order
import com.example.bookstore.network.SessionManager
import com.example.bookstore.repository.OrderRepository
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository     = OrderRepository(application, sessionManager)

    // Reactive stream — auto-updates when DB changes
    val orders: LiveData<List<Order>> = repository.observeOrders().asLiveData()

    private val _isLoading  = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _orderState = MutableLiveData<Result<String>>()
    val orderState: LiveData<Result<String>> = _orderState

    private val _cancelState = MutableLiveData<Result<String>?>()
    val cancelState: LiveData<Result<String>?> = _cancelState

    // Call once on screen open to populate cache from network
    fun refreshOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshOrders()
            _isLoading.value = false
        }
    }

    fun placeOrder(cartItems: List<CartEntity>, shippingAddress: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.placeOrder(cartItems, shippingAddress)
            if (result.isSuccess) {
                _orderState.value = Result.success("Order placed successfully!")
                refreshOrders()
            } else {
                _orderState.value = Result.failure(result.exceptionOrNull()!!)
            }
            _isLoading.value = false
        }
    }

    fun cancelOrder(orderId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _cancelState.value = repository.cancelOrder(orderId)
            _isLoading.value = false
        }
    }

    fun consumeCancelState() {
        _cancelState.value = null
    }
}
