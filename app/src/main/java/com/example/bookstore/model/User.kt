package com.example.bookstore.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("name")    val name: String
)