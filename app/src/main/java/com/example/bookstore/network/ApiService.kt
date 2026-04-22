package com.example.bookstore.network

import com.example.bookstore.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/register.php")
    suspend fun register(@Body body: Map<String, String>): Response<MessageResponse>

    @POST("api/auth/login.php")
    suspend fun login(@Body body: Map<String, String>): Response<AuthResponse>

    // Books
    @GET("api/books/index.php")
    suspend fun getBooks(
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("since")       since: String? = null
    ): Response<BooksResponse>

    @GET("api/books/show.php")
    suspend fun getBook(@Query("id") bookId: Int): Response<BookResponse>

    // Categories
    @GET("api/categories/index.php")
    suspend fun getCategories(): Response<CategoriesResponse>

    // Orders
    @POST("api/orders/create.php")
    suspend fun placeOrder(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<PlaceOrderResponse>

    @GET("api/orders/index.php")
    suspend fun getOrders(
        @Header("Authorization") token: String,
        @Query("since") since: String? = null
    ): Response<OrdersResponse>

    @POST("api/orders/cancel.php")
    suspend fun cancelOrder(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<MessageResponse>



    // ── Reviews ───────────────────────────────────────────────────────────────────

    @GET("api/reviews/index.php")
    suspend fun getReviews(
        @Query("book_id") bookId: Int
    ): ReviewsResponse

    @POST("api/reviews/create.php")
    suspend fun submitReview(
        @Header("Authorization") token: String,
        @Body body: SubmitReviewRequest
    ): SubmitReviewResponse

    @GET("api/reviews/check_eligibility.php")
    suspend fun checkReviewEligibility(
        @Header("Authorization") token: String,
        @Query("book_id") bookId: Int
    ): EligibilityResponse


    // ── Ebook ─────────────────────────────────────────────────────────────────────
    @GET("api/ebooks/get_pdf.php")
    suspend fun getPdfUrl(
        @Header("Authorization") token: String,
        @Query("book_id") bookId: Int
    ): PdfUrlResponse
}
