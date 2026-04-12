package com.example.bookstore

import android.app.Application
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.worker.BookSyncWorker

class BookstoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.appContext = this
        BookSyncWorker.schedule(this)
    }
}