package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.repository.BookRepository
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    private val _books      = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> = _selectedBook

    private val _categories  = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _isLoading   = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error       = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadBooks(search: String? = null, categoryId: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getBooks(search, categoryId)
            _books.value = result
            _isLoading.value = false
        }
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.refreshBooks()
            _books.value = result
            _isLoading.value = false
        }
    }

    fun loadBookById(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedBook.value = repository.getBookById(id)
            _isLoading.value = false
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val result = repository.getCategories()
            _categories.value = result
        }
    }

    fun refreshCategories() {
        viewModelScope.launch {
            val result = repository.refreshCategories()
            _categories.value = result
        }
    }
}