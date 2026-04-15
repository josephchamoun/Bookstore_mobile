package com.example.bookstore

import android.app.Application
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.worker.BookSyncWorker
import com.example.bookstore.worker.OrderSyncWorker

class BookstoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.appContext = this
        BookSyncWorker.schedule(this)
        OrderSyncWorker.scheduleNow(this)
    }
}
