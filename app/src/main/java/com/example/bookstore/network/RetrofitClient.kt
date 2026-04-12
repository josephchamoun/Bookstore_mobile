package com.example.bookstore.network

import android.content.Intent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2/bookstore_api/"
    const val ACTION_UNAUTHORIZED = "com.example.bookstore.UNAUTHORIZED"

    lateinit var appContext: android.content.Context

    private val authInterceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        if (response.code() == 401) {
            val intent = Intent(ACTION_UNAUTHORIZED)
            appContext.sendBroadcast(intent)
        }
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}