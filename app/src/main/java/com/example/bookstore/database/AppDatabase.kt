package com.example.bookstore.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        CategoryEntity::class,
        CartEntity::class,
        OrderEntity::class,
        PendingOrderEntity::class,
        FavoriteBookEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun pendingOrderDao(): PendingOrderDao
    abstract fun favoriteBookDao(): FavoriteBookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookstore_db"
                ).fallbackToDestructiveMigration() // 👈 ADD THIS

                    .build().also { INSTANCE = it }
            }
        }
    }
}
