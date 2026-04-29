package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.repository.BookRepository
import kotlinx.coroutines.launch

enum class BookSortOption(val label: String) {
    NEWEST("Newest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    TITLE("Title")
}

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository()

    private val sourceBooks = repository.getBooks().asLiveData()
    private val _books = MediatorLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    val categories: LiveData<List<Category>> = repository.getCategories().asLiveData()
    val favoriteBooks: LiveData<List<Book>>   = repository.getFavoriteBooks().asLiveData()

    private val _selectedBook = MutableLiveData<Book?>()
    val selectedBook: LiveData<Book?> = _selectedBook

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var latestBooks: List<Book> = emptyList()
    private var searchQuery: String = ""
    private var selectedCategoryId: String? = null
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

    // Firestore listener handles sync automatically
    // Keep refresh() for swipe-to-refresh UI compatibility
    fun refresh() {
        _isLoading.value = true
        _isLoading.value = false
    }

    fun loadBookById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedBook.value = repository.getBookById(id)
            _isLoading.value = false
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            repository.toggleFavorite(book.bookId)
            if (_selectedBook.value?.bookId == book.bookId) {
                _selectedBook.value = repository.getBookById(book.bookId)
            }
        }
    }

    fun updateQuery(query: String) { searchQuery = query.trim(); applyFilters() }
    fun updateCategory(categoryId: String?) { selectedCategoryId = categoryId; applyFilters() }
    fun updateInStockOnly(enabled: Boolean) { inStockOnly = enabled; applyFilters() }
    fun updateSortOption(option: BookSortOption) { sortOption = option; applyFilters() }

    fun updatePriceRange(min: String, max: String) {
        minPrice = min.toDoubleOrNull()
        maxPrice = max.toDoubleOrNull()
        applyFilters()
    }

    fun clearFilters() {
        searchQuery = ""; selectedCategoryId = null
        minPrice = null; maxPrice = null
        inStockOnly = false; sortOption = BookSortOption.NEWEST
        applyFilters()
    }

    fun clearError() { _error.value = null }

    private fun applyFilters() {
        val query = searchQuery.lowercase()
        _books.value = latestBooks
            .asSequence()
            .filter { query.isBlank() || it.title.lowercase().contains(query) || it.author.lowercase().contains(query) }
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .filter { minPrice == null || it.price >= minPrice!! }
            .filter { maxPrice == null || it.price <= maxPrice!! }
            .filter { !inStockOnly || it.stock > 0 }
            .toList()
            .let { books ->
                when (sortOption) {
                    BookSortOption.NEWEST         -> books.sortedByDescending { it.bookId }
                    BookSortOption.PRICE_LOW_HIGH -> books.sortedBy { it.price }
                    BookSortOption.PRICE_HIGH_LOW -> books.sortedByDescending { it.price }
                    BookSortOption.TITLE          -> books.sortedBy { it.title.lowercase() }
                }
            }
    }
}