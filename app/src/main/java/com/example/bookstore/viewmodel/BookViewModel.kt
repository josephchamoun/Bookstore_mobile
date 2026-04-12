package com.example.bookstore.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.repository.BookRepository
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(
        bookDao = AppDatabase.getInstance(application).bookDao(),
        catDao  = AppDatabase.getInstance(application).categoryDao(),
        api     = RetrofitClient.instance
    )

    // ── Reactive streams (auto-update when DB changes) ────────────────────

    val books: LiveData<List<Book>> = repository.getBooks().asLiveData()

    val categories: LiveData<List<Category>> = repository.getCategories().asLiveData()

    // ── One-off state ─────────────────────────────────────────────────────

    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> = _selectedBook

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ── Actions ───────────────────────────────────────────────────────────

    // Called by SwipeRefresh — triggers network fetch, which upserts into
    // Room, which makes the Flow above re-emit automatically
    fun refresh() {
        viewModelScope.launch {
            Log.d("BookViewModel", "refresh started")
            _isLoading.value = true
            try {
                repository.refreshBooksFromNetwork()
                repository.refreshCategoriesFromNetwork()
                Log.d("BookViewModel", "refresh done")
            } catch (e: Exception) {
                Log.e("BookViewModel", "refresh error: ${e.message}", e)
                _error.value = "Failed to refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBookById(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedBook.value = repository.getBookById(id)
            _isLoading.value = false
        }
    }

    // Search and category filter — read directly from local DB (instant, no network)
    fun searchBooks(query: String): LiveData<List<Book>> {
        val result = MutableLiveData<List<Book>>()
        viewModelScope.launch {
            result.value = repository.getBooksBySearch(query)
        }
        return result
    }

    fun filterByCategory(categoryId: Int): LiveData<List<Book>> {
        val result = MutableLiveData<List<Book>>()
        viewModelScope.launch {
            result.value = repository.getBooksByCategory(categoryId)
        }
        return result
    }

    fun clearError() {
        _error.value = null
    }
}