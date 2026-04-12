package com.example.bookstore.repository

import android.util.Log
import com.example.bookstore.database.BookDao
import com.example.bookstore.database.CategoryDao
import com.example.bookstore.database.BookEntity
import com.example.bookstore.database.CategoryEntity
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class BookRepository(
    private val bookDao: BookDao,
    private val catDao: CategoryDao,
    private val api: ApiService
) {

    // ───────────────────────────────
    // 📚 BOOKS (UI LAYER - REACTIVE)
    // ───────────────────────────────

    fun getBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { entities ->
            entities.map { it.toBook() }
        }

    suspend fun getBooksBySearch(search: String): List<Book> {
        return bookDao.searchBooks(search).map { it.toBook() }
    }

    suspend fun getBooksByCategory(categoryId: Int): List<Book> {
        return bookDao.getBooksByCategory(categoryId).map { it.toBook() }
    }

    suspend fun getBookById(id: Int): Book? {
        return bookDao.getBookById(id)?.toBook()
    }

    // ───────────────────────────────
    // 👷 WORKER ONLY (SYNC NETWORK)
    // ───────────────────────────────

    suspend fun refreshBooksFromNetwork() {
        try {
            val response = api.getBooks()
            Log.d("BookRepo", "response code: ${response.code()}")
            Log.d("BookRepo", "books count: ${response.body()?.books?.size}")
            if (response.isSuccessful) {
                val books = response.body()?.books ?: return
                val now = Instant.now().toString()
                val freshIds = books.map { it.bookId }.toSet()

                // Upsert all fresh books
                bookDao.upsertAll(books.map { it.toEntity(now) })

                // Delete any local books that no longer exist on the server
                bookDao.deleteAbsent(freshIds.toList())

                Log.d("BookRepo", "upserted ${books.size} books")
            }
        } catch (e: Exception) {
            Log.e("BookRepo", "exception: ${e.message}", e)
        }
    }

    suspend fun refreshCategoriesFromNetwork() {
        try {
            val response = api.getCategories()
            if (response.isSuccessful) {
                val cats = response.body()?.categories ?: return
                val now = Instant.now().toString()
                // insertAll already has OnConflictStrategy.REPLACE so this is an upsert
                catDao.insertAll(
                    cats.map {
                        CategoryEntity(
                            categoryId = it.categoryId,
                            name = it.name,
                            cachedAt = now
                        )
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ───────────────────────────────
    // 📦 CATEGORIES
    // ───────────────────────────────

    fun getCategories(): Flow<List<Category>> =
        catDao.getAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }

    // ───────────────────────────────
    // 🔁 MAPPERS
    // ───────────────────────────────

    private fun BookEntity.toBook() = Book(
        bookId = bookId,
        categoryId = categoryId,
        title = title,
        author = author,
        price = price,
        stock = stock,
        coverUrl = coverUrl,
        categoryName = categoryName
    )

    private fun Book.toEntity(cachedAt: String) = BookEntity(
        bookId = bookId,
        categoryId = categoryId,
        title = title,
        author = author,
        price = price,
        stock = stock,
        coverUrl = coverUrl,
        categoryName = categoryName,
        cachedAt = cachedAt
    )

    private fun CategoryEntity.toCategory() = Category(
        categoryId = categoryId,
        name = name
    )
}