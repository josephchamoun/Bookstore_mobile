package com.example.bookstore.repository

import android.util.Log
import com.example.bookstore.database.BookDao
import com.example.bookstore.database.BookEntity
import com.example.bookstore.database.CategoryDao
import com.example.bookstore.database.CategoryEntity
import com.example.bookstore.database.FavoriteBookDao
import com.example.bookstore.database.FavoriteBookEntity
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.example.bookstore.network.ApiService
import com.example.bookstore.network.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

class BookRepository(
    private val bookDao: BookDao,
    private val catDao: CategoryDao,
    private val favoriteDao: FavoriteBookDao,
    private val api: ApiService,
    private val session: SessionManager
) {

    fun getBooks(): Flow<List<Book>> {
        val userId = session.getUserId()
        return combine(bookDao.getAllBooks(), favoriteDao.observeFavoriteIds(userId)) { entities, favoriteIds ->
            val favoriteSet = favoriteIds.toSet()
            entities.map { it.toBook(isFavorite = it.bookId in favoriteSet) }
        }
    }

    fun getFavoriteBooks(): Flow<List<Book>> {
        val userId = session.getUserId()
        return combine(bookDao.getAllBooks(), favoriteDao.observeFavoriteIds(userId)) { entities, favoriteIds ->
            val favoriteSet = favoriteIds.toSet()
            entities
                .filter { it.bookId in favoriteSet }
                .map { it.toBook(isFavorite = true) }
                .sortedBy { it.title.lowercase() }
        }
    }

    suspend fun toggleFavorite(bookId: Int): Boolean {
        val userId = session.getUserId()
        val currentlyFavorite = favoriteDao.isFavorite(userId, bookId)
        if (currentlyFavorite) {
            favoriteDao.delete(userId, bookId)
        } else {
            favoriteDao.insert(FavoriteBookEntity(userId = userId, bookId = bookId))
        }
        return !currentlyFavorite
    }

    suspend fun getBookById(id: Int): Book? {
        val userId = session.getUserId()
        val isFavorite = favoriteDao.isFavorite(userId, id)
        return bookDao.getBookById(id)?.toBook(isFavorite = isFavorite)
    }

    suspend fun refreshBooksFromNetwork() {
        try {
            val response = api.getBooks()
            Log.d("BookRepo", "response code: ${response.code()}")
            Log.d("BookRepo", "books count: ${response.body()?.books?.size}")
            if (response.isSuccessful) {
                val books = response.body()?.books ?: return
                val now = Instant.now().toString()
                val freshIds = books.map { it.bookId }.toSet()
                bookDao.upsertAll(books.map { it.toEntity(now) })
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

    fun getCategories(): Flow<List<Category>> =
        catDao.getAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }

    suspend fun isFavorite(bookId: Int): Boolean {
        return favoriteDao.isFavorite(session.getUserId(), bookId)
    }

    suspend fun getFavoriteCount(): Int {
        return favoriteDao.observeFavoriteIds(session.getUserId()).first().size
    }

    private fun BookEntity.toBook(isFavorite: Boolean) = Book(
        bookId       = bookId,
        categoryId   = categoryId,
        title        = title,
        author       = author,
        price        = price,
        stock        = stock,
        coverUrl     = coverUrl,
        categoryName = categoryName,
        hasEbook     = if (hasEbook) 1 else 0,
        isFavorite   = isFavorite
    )

    private fun Book.toEntity(cachedAt: String) = BookEntity(
        bookId       = bookId,
        categoryId   = categoryId,
        title        = title,
        author       = author,
        price        = price,
        stock        = stock,
        coverUrl     = coverUrl,
        categoryName = categoryName,
        cachedAt     = cachedAt,
        hasEbook     = hasEbook == 1
    )

    private fun CategoryEntity.toCategory() = Category(
        categoryId = categoryId,
        name = name
    )
}
