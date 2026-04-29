package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.model.Order
import com.example.bookstore.repository.OrderRepository
import kotlinx.coroutines.launch

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrderRepository()

    val orders: LiveData<List<Order>> = repository.getOrders().asLiveData()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _cancelState = MutableLiveData<Result<String>?>()
    val cancelState: LiveData<Result<String>?> = _cancelState

    // No manual refresh needed — Firestore listener auto-updates
    fun refreshOrders() { }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _cancelState.value = repository.cancelOrder(orderId)
            _isLoading.value = false
        }
    }

    fun consumeCancelState() { _cancelState.value = null }
}