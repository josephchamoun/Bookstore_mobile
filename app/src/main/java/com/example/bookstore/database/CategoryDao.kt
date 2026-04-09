package com.example.bookstore.database

import androidx.room.*

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories_cache ORDER BY name")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories_cache")
    suspend fun clearAll()
}