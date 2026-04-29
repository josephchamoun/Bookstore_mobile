package com.example.bookstore.repository

import com.example.bookstore.auth.SessionManager
import com.example.bookstore.model.Book
import com.example.bookstore.model.Category
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookRepository {
    private val db = Firebase.firestore

    // ── Books stream (replaces Room Flow + WorkManager sync) ──────────────
    fun getBooks(): Flow<List<Book>> = callbackFlow {
        val userId = SessionManager.getCurrentUserId()
        val listener = db.collection("books")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val books = snapshot?.documents?.map { doc ->
                    Book(
                        bookId       = doc.id,
                        title        = doc.getString("title") ?: "",
                        author       = doc.getString("author") ?: "",
                        price        = doc.getDouble("price") ?: 0.0,
                        stock        = (doc.getLong("stock") ?: 0L).toInt(),
                        coverUrl     = doc.getString("coverUrl") ?: "",
                        categoryId   = doc.getString("categoryId") ?: "",
                        categoryName = doc.getString("categoryName") ?: "",
                        hasEbook     = doc.getBoolean("hasEbook") ?: false,
                        ebookUrl     = doc.getString("ebookUrl") ?: "",
                        isFavorite   = (doc.get("favorites") as? List<*>)?.contains(userId) == true
                    )
                } ?: emptyList()
                trySend(books)
            }
        awaitClose { listener.remove() }
    }

    // ── Categories stream ─────────────────────────────────────────────────
    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val cats = snapshot?.documents?.map { doc ->
                    Category(
                        categoryId = doc.id,
                        name       = doc.getString("name") ?: ""
                    )
                } ?: emptyList()
                trySend(cats)
            }
        awaitClose { listener.remove() }
    }

    // ── Favorites ─────────────────────────────────────────────────────────
    suspend fun toggleFavorite(bookId: String): Boolean {
        val userId = SessionManager.getCurrentUserId()
        val ref = db.collection("books").document(bookId)
        val doc = ref.get().await()
        val favorites = (doc.get("favorites") as? List<*>)?.map { it.toString() }?.toMutableList()
            ?: mutableListOf()
        return if (favorites.contains(userId)) {
            favorites.remove(userId)
            ref.update("favorites", favorites).await()
            false
        } else {
            favorites.add(userId)
            ref.update("favorites", favorites).await()
            true
        }
    }

    suspend fun isFavorite(bookId: String): Boolean {
        val userId = SessionManager.getCurrentUserId()
        val doc = db.collection("books").document(bookId).get().await()
        val favorites = (doc.get("favorites") as? List<*>)?.map { it.toString() } ?: emptyList()
        return favorites.contains(userId)
    }

    fun getFavoriteBooks(): Flow<List<Book>> = callbackFlow {
        val userId = SessionManager.getCurrentUserId()
        val listener = db.collection("books")
            .whereArrayContains("favorites", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val books = snapshot?.documents?.map { doc ->
                    Book(
                        bookId       = doc.id,
                        title        = doc.getString("title") ?: "",
                        author       = doc.getString("author") ?: "",
                        price        = doc.getDouble("price") ?: 0.0,
                        stock        = (doc.getLong("stock") ?: 0L).toInt(),
                        coverUrl     = doc.getString("coverUrl") ?: "",
                        categoryId   = doc.getString("categoryId") ?: "",
                        categoryName = doc.getString("categoryName") ?: "",
                        hasEbook     = doc.getBoolean("hasEbook") ?: false,
                        ebookUrl     = doc.getString("ebookUrl") ?: "",
                        isFavorite   = true
                    )
                } ?: emptyList()
                trySend(books)
            }
        awaitClose { listener.remove() }
    }

    // ── Single book ───────────────────────────────────────────────────────
    suspend fun getBookById(bookId: String): Book? {
        val userId = SessionManager.getCurrentUserId()
        val doc = db.collection("books").document(bookId).get().await()
        if (!doc.exists()) return null
        val favorites = (doc.get("favorites") as? List<*>)?.map { it.toString() } ?: emptyList()
        return Book(
            bookId       = doc.id,
            title        = doc.getString("title") ?: "",
            author       = doc.getString("author") ?: "",
            price        = doc.getDouble("price") ?: 0.0,
            stock        = (doc.getLong("stock") ?: 0L).toInt(),
            coverUrl     = doc.getString("coverUrl") ?: "",
            categoryId   = doc.getString("categoryId") ?: "",
            categoryName = doc.getString("categoryName") ?: "",
            hasEbook     = doc.getBoolean("hasEbook") ?: false,
            ebookUrl     = doc.getString("ebookUrl") ?: "",
            isFavorite   = favorites.contains(userId)
        )
    }
}