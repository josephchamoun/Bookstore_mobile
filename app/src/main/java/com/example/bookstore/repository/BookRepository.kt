package com.example.bookstore.repository

import android.content.Context
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.database.BookEntity
import com.example.bookstore.database.CategoryEntity
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.network.RetrofitClient
import java.time.Instant

class BookRepository(context: Context) {

    private val api      = RetrofitClient.instance
    private val bookDao  = AppDatabase.getInstance(context).bookDao()
    private val catDao   = AppDatabase.getInstance(context).categoryDao()

    // ── Books ──────────────────────────────────────────────

    suspend fun getBooks(
        search: String? = null,
        categoryId: Int? = null
    ): List<Book> {
        // Return cache first
        val cached = when {
            !search.isNullOrBlank() -> bookDao.searchBooks(search)
            categoryId != null      -> bookDao.getBooksByCategory(categoryId)
            else                    -> bookDao.getAllBooks()
        }
        if (cached.isNotEmpty()) return cached.map { it.toBook() }

        // Fetch from API if cache empty
        return fetchAndCacheBooks()
    }

    suspend fun refreshBooks(): List<Book> = fetchAndCacheBooks()

    private suspend fun fetchAndCacheBooks(): List<Book> {
        return try {
            val response = api.getBooks()
            if (response.isSuccessful) {
                val books = response.body()?.books ?: emptyList()
                val now   = Instant.now().toString()
                bookDao.clearAll()
                bookDao.insertAll(books.map { it.toEntity(now) })
                books
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBookById(id: Int): Book? {
        return try {
            val response = api.getBook(id)
            if (response.isSuccessful) response.body()?.book
            else bookDao.getBookById(id)?.toBook()
        } catch (e: Exception) {
            bookDao.getBookById(id)?.toBook()
        }
    }

    // ── Categories ─────────────────────────────────────────

    suspend fun getCategories(): List<Category> {
        val cached = catDao.getAllCategories()
        if (cached.isNotEmpty()) return cached.map { it.toCategory() }
        return fetchAndCacheCategories()
    }

    suspend fun refreshCategories(): List<Category> = fetchAndCacheCategories()

    private suspend fun fetchAndCacheCategories(): List<Category> {
        return try {
            val response = api.getCategories()
            if (response.isSuccessful) {
                val cats = response.body()?.categories ?: emptyList()
                val now  = Instant.now().toString()
                catDao.clearAll()
                catDao.insertAll(cats.map { CategoryEntity(it.categoryId, it.name, now) })
                cats
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Mappers ────────────────────────────────────────────

    private fun Book.toEntity(cachedAt: String) = BookEntity(
        bookId       = bookId,
        categoryId   = categoryId,
        title        = title,
        author       = author,
        price        = price,
        stock        = stock,
        coverUrl     = coverUrl,
        categoryName = categoryName,
        cachedAt     = cachedAt
    )

    private fun BookEntity.toBook() = Book(
        bookId       = bookId,
        categoryId   = categoryId,
        title        = title,
        author       = author,
        price        = price,
        stock        = stock,
        coverUrl     = coverUrl,
        categoryName = categoryName
    )

    private fun CategoryEntity.toCategory() = Category(
        categoryId = categoryId,
        name       = name
    )
}