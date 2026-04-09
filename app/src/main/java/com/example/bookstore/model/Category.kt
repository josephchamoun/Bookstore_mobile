package com.example.bookstore.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("c_name")      val name: String
)