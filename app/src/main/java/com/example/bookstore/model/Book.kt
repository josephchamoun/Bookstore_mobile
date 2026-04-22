package com.example.bookstore.model

import com.google.gson.annotations.SerializedName

data class Book(
    @SerializedName("book_id")     val bookId: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("b_title")     val title: String,
    @SerializedName("b_author")    val author: String,
    @SerializedName("b_price")     val price: Double,
    @SerializedName("b_stock")     val stock: Int,
    @SerializedName("b_cover_url") val coverUrl: String?,
    @SerializedName("c_name")      val categoryName: String?,
    @SerializedName("has_ebook") val hasEbook: Int = 0,
    @SerializedName("updated_at")  val updatedAt: String? = null,
    val isFavorite: Boolean = false
)
