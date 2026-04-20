package com.example.bookstore.repository

import android.content.Context
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import java.io.File
import java.io.IOException

class EbookRepository(context: Context) {

    private val api           = RetrofitClient.instance
    private val sessionManager = SessionManager(context)
    private val cacheDir      = context.cacheDir

    // ── Fetch PDF URL from API, then download PDF to local cache ─────────────
    suspend fun loadPdf(bookId: Int): PdfResult {
        return try {
            val token    = sessionManager.getBearerToken()
            val response = api.getPdfUrl(token, bookId)

            android.util.Log.d("EbookDebug", "success=${response.success}")
            android.util.Log.d("EbookDebug", "pdf_url=${response.pdfUrl}")
            android.util.Log.d("EbookDebug", "message=${response.message}")

            if (!response.success || response.pdfUrl == null) {
                return PdfResult.Error(response.message ?: "Could not load ebook")
            }

            // Check if already cached
            val cachedFile = File(cacheDir, "ebook_$bookId.pdf")
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return PdfResult.Ready(cachedFile)
            }

            // Download PDF to cache
            val downloaded = downloadPdf(response.pdfUrl, cachedFile)
            if (downloaded) {
                PdfResult.Ready(cachedFile)
            } else {
                PdfResult.Error("Failed to download ebook")
            }

        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                403 -> "You can only read ebooks from your delivered orders"
                404 -> "This book has no ebook available"
                401 -> "Please log in again"
                else -> "Something went wrong (${e.code()})"
            }
            PdfResult.Error(message)
        } catch (e: IOException) {
            PdfResult.Error("No internet connection")
        } catch (e: Exception) {
            PdfResult.Error("Unexpected error: ${e.message}")
        }
    }

    // ── Download PDF bytes into a local cache file ────────────────────────────
    private suspend fun downloadPdf(url: String, destFile: File): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client  = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Authorization", sessionManager.getBearerToken())
                    .build()

                val call     = client.newCall(request)
                val response = call.execute() // blocking — we're on IO dispatcher

                if (!response.isSuccessful) {
                    response.close()
                    return@withContext false
                }

                val inputStream = response.body()?.byteStream()
                    ?: run { response.close(); return@withContext false }

                destFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }

                response.close()
                true

            } catch (e: Exception) {
                android.util.Log.e("EbookRepo", "Download failed: ${e.message}")
                false
            }
        }
    }

    // ── Clear cached PDF for a book (optional — call if needed) ──────────────
    fun clearCache(bookId: Int) {
        File(cacheDir, "ebook_$bookId.pdf").delete()
    }
}

// ── Result sealed class ───────────────────────────────────────────────────────
sealed class PdfResult {
    data class Ready(val file: File)      : PdfResult()
    data class Error(val message: String) : PdfResult()
}