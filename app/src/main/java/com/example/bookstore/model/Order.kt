package com.example.bookstore.model

import com.google.gson.annotations.SerializedName

data class Order(
    @SerializedName("order_id")          val orderId: Int,
    @SerializedName("user_id")           val userId: Int,
    @SerializedName("order_date")        val orderDate: String,
    @SerializedName("o_total")           val total: Double,
    @SerializedName("o_status")          val status: String,
    @SerializedName("o_shipping_address") val shippingAddress: String,
    @SerializedName("items")             val items: List<OrderItem>?,
    val isSynced: Boolean = true
)

data class OrderItem(
    @SerializedName("item_id")       val itemId: Int,
    @SerializedName("order_id")      val orderId: Int,
    @SerializedName("book_id")       val bookId: Int,
    @SerializedName("oi_quantity")   val quantity: Int,
    @SerializedName("oi_unit_price") val unitPrice: Double,
    @SerializedName("b_title")       val bookTitle: String?,
    @SerializedName("b_author")      val bookAuthor: String?,
    @SerializedName("b_cover_url")   val coverUrl: String?
)