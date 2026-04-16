package com.example.bookstore.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.example.bookstore.repository.BookRepository
import kotlinx.coroutines.launch

enum class BookSortOption(val label: String) {
    NEWEST("Newest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    TITLE("Title")
}

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(
        bookDao = AppDatabase.getInstance(application).bookDao(),
        catDao = AppDatabase.getInstance(application).categoryDao(),
        favoriteDao = AppDatabase.getInstance(application).favoriteBookDao(),
        api = RetrofitClient.instance,
        session = SessionManager(application)
    )

    private val sourceBooks = repository.getBooks().asLiveData()
    private val _books = MediatorLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    val categories: LiveData<List<Category>> = repository.getCategories().asLiveData()
    val favoriteBooks: LiveData<List<Book>> = repository.getFavoriteBooks().asLiveData()

    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> = _selectedBook

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var latestBooks: List<Book> = emptyList()
    private var searchQuery: String = ""
    private var selectedCategoryId: Int? = null
    private var minPrice: Double? = null
    private var maxPrice: Double? = null
    private var inStockOnly: Boolean = false
    private var sortOption: BookSortOption = BookSortOption.NEWEST

    init {
        _books.addSource(sourceBooks) { books ->
            latestBooks = books.orEmpty()
            applyFilters()
        }
    }

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

    fun updateQuery(query: String) {
        searchQuery = query.trim()
        applyFilters()
    }

    fun updateCategory(categoryId: Int?) {
        selectedCategoryId = categoryId
        applyFilters()
    }

    fun updatePriceRange(min: String, max: String) {
        minPrice = min.toDoubleOrNull()
        maxPrice = max.toDoubleOrNull()
        applyFilters()
    }

    fun updateInStockOnly(enabled: Boolean) {
        inStockOnly = enabled
        applyFilters()
    }

    fun updateSortOption(option: BookSortOption) {
        sortOption = option
        applyFilters()
    }

    fun clearFilters() {
        searchQuery = ""
        selectedCategoryId = null
        minPrice = null
        maxPrice = null
        inStockOnly = false
        sortOption = BookSortOption.NEWEST
        applyFilters()
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            repository.toggleFavorite(book.bookId)
            if (_selectedBook.value?.bookId == book.bookId) {
                _selectedBook.value = repository.getBookById(book.bookId)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun applyFilters() {
        val query = searchQuery.lowercase()
        val filtered = latestBooks
            .asSequence()
            .filter { book ->
                query.isBlank() ||
                    book.title.lowercase().contains(query) ||
                    book.author.lowercase().contains(query)
            }
            .filter { book ->
                selectedCategoryId == null || book.categoryId == selectedCategoryId
            }
            .filter { book ->
                minPrice == null || book.price >= minPrice!!
            }
            .filter { book ->
                maxPrice == null || book.price <= maxPrice!!
            }
            .filter { book ->
                !inStockOnly || book.stock > 0
            }
            .toList()
            .let { books ->
                when (sortOption) {
                    BookSortOption.NEWEST -> books.sortedByDescending { it.bookId }
                    BookSortOption.PRICE_LOW_HIGH -> books.sortedBy { it.price }
                    BookSortOption.PRICE_HIGH_LOW -> books.sortedByDescending { it.price }
                    BookSortOption.TITLE -> books.sortedBy { it.title.lowercase() }
                }
            }

        _books.value = filtered
    }
}
