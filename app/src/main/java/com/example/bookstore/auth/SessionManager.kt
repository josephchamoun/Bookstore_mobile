package com.example.bookstore.auth

import com.google.firebase.auth.FirebaseAuth

object SessionManager {
    private val auth = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    fun getCurrentUserEmail(): String = auth.currentUser?.email ?: ""

    fun logout() = auth.signOut()
}