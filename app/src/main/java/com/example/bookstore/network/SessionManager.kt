package com.example.bookstore.network

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bookstore_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOKEN   = "jwt_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME    = "user_name"
        const val KEY_ADDRESS = "user_address"
    }

    fun saveSession(token: String, userId: Int, name: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun saveAddress(address: String) {
        prefs.edit().putString(KEY_ADDRESS, address).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUserName(): String? = prefs.getString(KEY_NAME, null)

    fun getAddress(): String? = prefs.getString(KEY_ADDRESS, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() = prefs.edit().clear().apply()

    fun getBearerToken(): String = "Bearer ${getToken()}"

    fun logout() = prefs.edit().clear().apply()
}
