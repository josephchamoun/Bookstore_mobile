package com.example.bookstore.repository

import com.example.bookstore.model.AuthResponse
import com.example.bookstore.model.MessageResponse
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.example.bookstore.model.User

class AuthRepository(private val sessionManager: SessionManager) {

    private val api = RetrofitClient.instance

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(mapOf("email" to email, "password" to password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.token != null && body.user != null) {
                    sessionManager.saveSession(
                        token  = body.token,
                        userId = body.user.userId,
                        name   = body.user.name
                    )
                }
                Result.success(body)
            } else if (response.code() == 403) {

                val errorBody = response.errorBody()?.string()
                val json      = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                val reason    = json.get("ban_reason")?.asString ?: "You have been banned."
                Result.failure(Exception("banned:$reason"))
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        address: String
    ): Result<MessageResponse> {
        return try {
            val response = api.register(
                mapOf(
                    "name"     to name,
                    "email"    to email,
                    "password" to password,
                    "address"  to address
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}