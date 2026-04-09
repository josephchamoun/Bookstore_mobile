package com.example.bookstore.database

import androidx.room.*

@Dao
interface BookDao {

    @Query("SELECT * FROM books_cache ORDER BY title")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books_cache WHERE bookId = :id")
    suspend fun getBookById(id: Int): BookEntity?

    @Query("SELECT * FROM books_cache WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    suspend fun searchBooks(query: String): List<BookEntity>

    @Query("SELECT * FROM books_cache WHERE categoryId = :categoryId")
    suspend fun getBooksByCategory(categoryId: Int): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)

    @Query("DELETE FROM books_cache")
    suspend fun clearAll()
}