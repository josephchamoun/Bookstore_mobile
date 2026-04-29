package com.example.bookstore.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.bookstore.auth.SessionManager
import com.example.bookstore.model.Book
import com.example.bookstore.repository.BookRepository
import com.example.bookstore.repository.OrderRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository  = BookRepository()
    private val orderRepository = OrderRepository()
    private val prefs = application.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    private val _savedAddress = MutableLiveData(prefs.getString("address", "") ?: "")
    val savedAddress: LiveData<String> = _savedAddress

    private val _userName = MutableLiveData(Firebase.auth.currentUser?.displayName ?: "User")
    val userName: LiveData<String> = _userName

    val favoriteBooks = bookRepository.getFavoriteBooks().asLiveData()
    val orders        = orderRepository.getOrders().asLiveData()

    val favoriteCount: LiveData<String> = favoriteBooks.map { it.size.toString() }
    val orderCount: LiveData<String>    = orders.map { it.size.toString() }
    val pendingCount: LiveData<String>  = orders.map { list ->
        list.count { it.status.equals("pending", ignoreCase = true) }.toString()
    }

    fun saveAddress(address: String) {
        prefs.edit().putString("address", address).apply()
        viewModelScope.launch {
            Firebase.firestore.collection("users")
                .document(SessionManager.getCurrentUserId())
                .update("address", address).await()
        }
        _savedAddress.value = address
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            bookRepository.toggleFavorite(book.bookId)
        }
    }

    fun logout() = SessionManager.logout()
}