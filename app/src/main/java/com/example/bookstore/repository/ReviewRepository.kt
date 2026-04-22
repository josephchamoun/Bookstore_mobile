package com.example.bookstore.repository

import android.content.Context
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.database.PendingReviewEntity
import com.example.bookstore.database.ReviewEntity
import com.example.bookstore.model.EligibilityResponse
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import com.example.bookstore.model.SubmitReviewRequest

class ReviewRepository(context: Context) {

    private val reviewDao        = AppDatabase.getInstance(context).reviewDao()
    private val pendingReviewDao = AppDatabase.getInstance(context).pendingReviewDao()
    private val api              = RetrofitClient.instance
    private val sessionManager   = SessionManager(context)
    private val prefs            = context.getSharedPreferences("review_prefs", Context.MODE_PRIVATE)

    // Save/load average rating per book
    private fun saveAverageRating(bookId: Int, avg: Float) {
        prefs.edit().putFloat("avg_rating_$bookId", avg).apply()
    }

    fun getCachedAverageRating(bookId: Int): Float {
        return prefs.getFloat("avg_rating_$bookId", 0f)
    }

    // ── Observe cached reviews for a book — Flow never suspends ──────────────
    fun observeReviews(bookId: Int): Flow<List<ReviewEntity>> {
        return reviewDao.getReviewsByBook(bookId)
    }

    suspend fun refreshReviews(bookId: Int): Float {
        return try {
            val response = api.getReviews(bookId)
            if (response.success) {
                val entities = response.reviews.map { r ->
                    ReviewEntity(
                        reviewId  = r.reviewId,
                        bookId    = bookId,
                        userId    = r.userId,
                        userName  = r.userName,
                        rating    = r.rating,
                        comment   = r.comment,
                        status    = "approved",
                        createdAt = r.createdAt
                    )
                }
                reviewDao.upsertAll(entities)
                if (entities.isNotEmpty()) {
                    reviewDao.deleteAbsentForBook(bookId, entities.map { it.reviewId })
                }
                // Save to prefs so it survives offline
                saveAverageRating(bookId, response.averageRating)
            }
            response.averageRating
        } catch (e: Exception) {
            // Return cached average instead of 0f
            getCachedAverageRating(bookId)
        }
    }


    // ── Submit review — API first, fallback to offline queue ──────────────────
    suspend fun submitReview(bookId: Int, rating: Int, comment: String): SubmitResult {
        return try {
            val token    = sessionManager.getBearerToken()
            val response = api.submitReview(
                token = token,
                body  = SubmitReviewRequest(bookId, rating, comment)
            )
            if (response.success) {
                SubmitResult.Success(response.message)
            } else {
                SubmitResult.Error(response.message)
            }
        } catch (e: IOException) {
            // No network — save offline
            saveOffline(bookId, rating, comment)
            SubmitResult.SavedOffline
        } catch (e: retrofit2.HttpException) {
            // API returned 4xx/5xx — parse the error body
            val errorBody = e.response()?.errorBody()?.string()
            val message = try {
                val json = org.json.JSONObject(errorBody ?: "")
                json.getString("message")
            } catch (ex: Exception) {
                when (e.code()) {
                    403 -> "You can only review books from your delivered orders"
                    409 -> "You have already reviewed this book"
                    401 -> "Please log in again"
                    else -> "Something went wrong (${e.code()})"
                }
            }
            SubmitResult.Error(message)
        } catch (e: Exception) {
            SubmitResult.Error("Unexpected error: ${e.message}")
        }
    }

    // ── Save to pending_reviews for later sync ────────────────────────────────
    private suspend fun saveOffline(bookId: Int, rating: Int, comment: String) {
        pendingReviewDao.insert(
            PendingReviewEntity(
                bookId  = bookId,
                rating  = rating,
                comment = comment
            )
        )
    }

    // ── Called by ReviewSyncWorker for each pending review ────────────────────
    suspend fun submitPendingReview(pending: PendingReviewEntity): Boolean {
        return try {
            val token    = sessionManager.getBearerToken()
            val response = api.submitReview(
                token = token,
                body  = SubmitReviewRequest(pending.bookId, pending.rating, pending.comment)
            )
            // 409 = already reviewed — treat as success so it's removed from queue
            response.success
        } catch (e: IOException) {
            false
        }
    }

    // ── Pending review queue operations ───────────────────────────────────────
    fun observePendingReviews(): Flow<List<PendingReviewEntity>> {
        return pendingReviewDao.getAll()
    }

    suspend fun deletePendingReview(localId: Int) {
        pendingReviewDao.delete(localId)
    }

    suspend fun incrementRetry(localId: Int) {
        pendingReviewDao.incrementRetry(localId)
    }


    suspend fun checkEligibility(bookId: Int): EligibilityResponse {
        val token = sessionManager.getBearerToken()
        android.util.Log.d("EligibilityDebug", "Token: $token")
        android.util.Log.d("EligibilityDebug", "BookId: $bookId")
        return try {
            val result = api.checkReviewEligibility(token, bookId)
            android.util.Log.d("EligibilityDebug", "Result: eligible=${result.eligible}, reason=${result.reason}")
            result
        } catch (e: Exception) {
            android.util.Log.e("EligibilityDebug", "Error: ${e.message}")
            EligibilityResponse(false, false, "error")
        }
    }
}

// ── Result sealed class ───────────────────────────────────────────────────────
sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    object SavedOffline                     : SubmitResult()
    data class Error(val message: String)   : SubmitResult()
}