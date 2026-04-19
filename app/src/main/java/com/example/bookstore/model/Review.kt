package com.example.bookstore.model

import com.google.gson.annotations.SerializedName

data class Review(
    @SerializedName("review_id")  val reviewId: Int,
    @SerializedName("user_id")    val userId: Int,
    @SerializedName("user_name")  val userName: String,
    @SerializedName("rating")     val rating: Int,
    @SerializedName("comment")    val comment: String,
    @SerializedName("created_at") val createdAt: String
)

data class ReviewsResponse(
    @SerializedName("success")        val success: Boolean,
    @SerializedName("average_rating") val averageRating: Float,
    @SerializedName("review_count")   val reviewCount: Int,
    @SerializedName("reviews")        val reviews: List<Review>
)

data class SubmitReviewResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class SubmitReviewRequest(
    @SerializedName("book_id") val bookId: Int,
    @SerializedName("rating")  val rating: Int,
    @SerializedName("comment") val comment: String
)

data class EligibilityResponse(
    @SerializedName("success")  val success: Boolean,
    @SerializedName("eligible") val eligible: Boolean,
    @SerializedName("reason")   val reason: String
)