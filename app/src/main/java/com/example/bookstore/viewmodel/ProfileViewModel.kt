package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.model.Book
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.example.bookstore.repository.BookRepository
import com.example.bookstore.repository.OrderRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val bookRepository = BookRepository(
        bookDao = AppDatabase.getInstance(application).bookDao(),
        catDao = AppDatabase.getInstance(application).categoryDao(),
        favoriteDao = AppDatabase.getInstance(application).favoriteBookDao(),
        api = RetrofitClient.instance,
        session = SessionManager(application)
    )
    private val orderRepository = OrderRepository(application, sessionManager)

    private val _savedAddress = MutableLiveData(sessionManager.getAddress().orEmpty())
    val savedAddress: LiveData<String> = _savedAddress

    val userName: LiveData<String> = MutableLiveData(sessionManager.getUserName().orEmpty())
    val userId: LiveData<String> = MutableLiveData(
        sessionManager.getUserId().takeIf { it > 0 }?.toString().orEmpty()
    )
    val favoriteBooks = bookRepository.getFavoriteBooks().asLiveData()
    val orders = orderRepository.observeOrders().asLiveData()
    val favoriteCount: LiveData<String> = favoriteBooks.map { it.size.toString() }
    val orderCount: LiveData<String> = orders.map { it.size.toString() }
    val pendingCount: LiveData<String> = orders.map { list ->
        list.count { it.status.equals("pending", ignoreCase = true) }.toString()
    }

    fun saveAddress(address: String) {
        sessionManager.saveAddress(address)
        _savedAddress.value = address
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            bookRepository.toggleFavorite(book.bookId)
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            orderRepository.refreshOrders()
        }
    }

    fun logout() {
        sessionManager.logout()
    }
}
