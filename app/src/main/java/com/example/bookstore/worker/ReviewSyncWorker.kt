package com.example.bookstore.worker

import android.content.Context

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.repository.ReviewRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReviewSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = ReviewRepository(applicationContext)

        return try {
            // ── Grab current snapshot of pending reviews ──────────────────────
            // .first() collects one emission from the Flow then cancels —
            // identical pattern to OrderSyncWorker
            val pendingReviews = repository.observePendingReviews().first()

            if (pendingReviews.isEmpty()) return Result.success()

            var hadFailure = false

            pendingReviews.forEach { pending ->
                val submitted = repository.submitPendingReview(pending)

                if (submitted) {
                    // ── Success — remove from queue ───────────────────────────
                    repository.deletePendingReview(pending.localId)
                } else {
                    // ── Failed — increment retry count, signal retry needed ───
                    repository.incrementRetry(pending.localId)
                    hadFailure = true
                }
            }

            // If any review failed to sync, return retry so WorkManager
            // reschedules with LINEAR backoff
            if (hadFailure) Result.retry() else Result.success()

        } catch (e: Exception) {
            // Unexpected error — retry later
            Result.retry()
        }
    }

    companion object {
        fun scheduleNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ReviewSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}