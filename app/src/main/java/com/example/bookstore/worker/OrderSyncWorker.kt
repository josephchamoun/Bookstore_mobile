package com.example.bookstore.worker

import android.content.Context
import androidx.work.*
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.example.bookstore.repository.OrderRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class OrderSyncWorker(
        context: Context,
        params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db              = AppDatabase.getInstance(context)
    private val pendingOrderDao = db.pendingOrderDao()
    private val orderRepository = OrderRepository(
            context        = context,
            sessionManager = SessionManager(context)
    )

    override suspend fun doWork(): Result {
        val pending = pendingOrderDao.getAll().first()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false

        for (order in pending) {
            try {
                orderRepository.submitPendingOrder(order)
                pendingOrderDao.delete(order)
            } catch (e: Exception) {
                e.printStackTrace()
                pendingOrderDao.incrementRetry(order.localId)
                anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        const val WORK_NAME = "order_sync_oneshot"

        fun scheduleNow(context: Context) {
            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val request = OneTimeWorkRequestBuilder<OrderSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 2, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
            )
        }
    }
}