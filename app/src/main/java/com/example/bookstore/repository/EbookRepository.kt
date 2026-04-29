package com.example.bookstore.repository

import android.content.Context
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.File

class EbookRepository(private val context: Context) {
    private val db      = Firebase.firestore
    private val storage = Firebase.storage
    private val cacheDir = context.cacheDir

    // ── Load PDF — checks local cache first, downloads from Firebase Storage
    suspend fun loadPdf(bookId: String): PdfResult {
        return try {
            // Check local cache first
            val cachedFile = File(cacheDir, "ebook_$bookId.pdf")
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return PdfResult.Ready(cachedFile)
            }

            // Get ebookUrl from Firestore
            val doc = db.collection("books").document(bookId).get().await()
            val hasEbook = doc.getBoolean("hasEbook") ?: false
            if (!hasEbook) return PdfResult.Error("This book has no ebook available")

            // Check eligibility — must have delivered order
            val eligible = checkEligibility(bookId)
            if (!eligible) return PdfResult.Error("You can only read ebooks from your delivered orders")

            val ebookUrl = doc.getString("ebookUrl")
                ?: return PdfResult.Error("Ebook URL not found")

            // Download from Firebase Storage
            val storageRef = storage.getReferenceFromUrl(ebookUrl)
            storageRef.getFile(cachedFile).await()

            PdfResult.Ready(cachedFile)
        } catch (e: Exception) {
            PdfResult.Error(e.message ?: "Failed to load ebook")
        }
    }

    private suspend fun checkEligibility(bookId: String): Boolean {
        return try {
            val userId = com.example.bookstore.auth.SessionManager.getCurrentUserId()
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
        } catch (e: Exception) { false }
    }

    fun clearCache(bookId: String) {
        File(cacheDir, "ebook_$bookId.pdf").delete()
    }
}

sealed class PdfResult {
    data class Ready(val file: File)        : PdfResult()
    data class Error(val message: String)   : PdfResult()
}