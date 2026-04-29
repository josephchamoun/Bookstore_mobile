package com.example.bookstore.repository

import com.example.bookstore.auth.SessionManager
import com.example.bookstore.model.Review
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val db = Firebase.firestore

    // ── Reactive reviews stream for a book ────────────────────────────────
    fun getReviews(bookId: String): Flow<List<Review>> = callbackFlow {
        val listener = db.collection("reviews")
            .whereEqualTo("bookId", bookId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val reviews = snapshot?.documents?.map { doc ->
                    Review(
                        reviewId  = doc.id,
                        bookId    = doc.getString("bookId") ?: "",
                        userId    = doc.getString("userId") ?: "",
                        userName  = doc.getString("userName") ?: "",
                        rating    = (doc.getLong("rating") ?: 0L).toInt(),
                        comment   = doc.getString("comment") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    // ── Average rating — computed from reviews ────────────────────────────
    suspend fun getAverageRating(bookId: String): Float {
        val snapshot = db.collection("reviews")
            .whereEqualTo("bookId", bookId)
            .get().await()
        if (snapshot.isEmpty) return 0f
        val avg = snapshot.documents
            .mapNotNull { it.getLong("rating")?.toFloat() }
            .average()
        return avg.toFloat()
    }

    // ── Submit review ─────────────────────────────────────────────────────
    // Firestore offline persistence handles queuing automatically
    suspend fun submitReview(
        bookId: String,
        rating: Int,
        comment: String
    ): SubmitResult {
        if (rating < 1 || rating > 5)
            return SubmitResult.Error("Please select a rating between 1 and 5")
        if (comment.isBlank())
            return SubmitResult.Error("Please write a comment")

        return try {
            val userId   = SessionManager.getCurrentUserId()
            val userName = Firebase.firestore.collection("users")
                .document(userId).get().await()
                .getString("name") ?: "User"

            // Check if already reviewed
            val existing = db.collection("reviews")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("userId", userId)
                .get().await()

            if (!existing.isEmpty) {
                return SubmitResult.Error("You have already reviewed this book")
            }

            db.collection("reviews").add(
                hashMapOf(
                    "bookId"    to bookId,
                    "userId"    to userId,
                    "userName"  to userName,
                    "rating"    to rating,
                    "comment"   to comment,
                    "createdAt" to Timestamp.now()
                )
            ).await()

            SubmitResult.Success("Review submitted successfully!")
        } catch (e: Exception) {
            SubmitResult.Error(e.message ?: "Failed to submit review")
        }
    }

    // ── Check eligibility — user must have a delivered order with this book
    suspend fun checkEligibility(bookId: String): Boolean {
        return try {
            val userId = SessionManager.getCurrentUserId()
            val orders = db.collection("orders")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "delivered")
                .get().await()

            orders.documents.any { doc ->
                val items = doc.get("items") as? List<*> ?: emptyList<Any>()
                items.any { item ->
                    (item as? Map<*, *>)?.get("bookId") == bookId
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}

sealed class SubmitResult {
    data class Success(val message: String) : SubmitResult()
    data class Error(val message: String)   : SubmitResult()
}