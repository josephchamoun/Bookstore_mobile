package com.example.bookstore

import android.app.Application

class BookstoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // ← CHANGED: removed RetrofitClient, BookSyncWorker, OrderSyncWorker
        // Firebase initializes automatically via google-services.json
        // No manual setup needed
    }
}