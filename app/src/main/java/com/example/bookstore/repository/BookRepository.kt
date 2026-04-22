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
            // 1. Get local latest timestamp
            val localLastUpdated = bookDao.getLatestUpdatedAt()

            // 2. Pass it to server — server returns only books changed after this
            val response = api.getBooks(since = localLastUpdated)
            Log.d("BookRepo", "response code: ${response.code()}")

            if (response.isSuccessful) {
                val body = response.body() ?: return
                val books = body.books ?: return

                // 3. Server returned empty list — nothing changed, we're done
                if (books.isEmpty()) {
                    Log.d("BookRepo", "No changes since $localLastUpdated, skipping")
                    return
                }

                val now = Instant.now().toString()
                bookDao.upsertAll(books.map { it.toEntity(now) })

                if (localLastUpdated == null) {
                    // 4a. First ever fetch — delete books that no longer exist on server
                    bookDao.deleteAbsent(books.map { it.bookId })
                    Log.d("BookRepo", "Full fetch, upserted ${books.size} books")
                } else {
                    // 4b. Partial fetch — don't touch books we didn't receive
                    Log.d("BookRepo", "Partial fetch, upserted ${books.size} changed books")
                }
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
        hasEbook     = hasEbook == 1,
        updatedAt    = updatedAt
    )

    private fun CategoryEntity.toCategory() = Category(
        categoryId = categoryId,
        name = name
    )
}
