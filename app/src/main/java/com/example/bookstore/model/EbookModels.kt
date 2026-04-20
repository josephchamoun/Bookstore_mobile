package com.example.bookstore.model

import com.google.gson.annotations.SerializedName

data class PdfUrlResponse(
    @SerializedName("success")    val success: Boolean,
    @SerializedName("pdf_url")    val pdfUrl: String?,
    @SerializedName("book_title") val bookTitle: String?,
    @SerializedName("message")    val message: String?
)