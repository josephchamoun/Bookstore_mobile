package com.example.bookstore.worker

import android.content.Context
import androidx.work.*
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.repository.BookRepository
import java.util.concurrent.TimeUnit

class BookSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val bookRepository = BookRepository(
        bookDao = db.bookDao(),
        catDao  = db.categoryDao(),
        api     = RetrofitClient.instance
    )

    override suspend fun doWork(): Result {
        return try {
            bookRepository.refreshBooksFromNetwork()
            bookRepository.refreshCategoriesFromNetwork()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "book_sync_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BookSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}