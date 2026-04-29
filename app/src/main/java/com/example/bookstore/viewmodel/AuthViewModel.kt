package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.auth.SessionManager
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    private val _loginState = MutableLiveData<Result<String>>()
    val loginState: LiveData<Result<String>> = _loginState

    private val _registerState = MutableLiveData<Result<String>>()
    val registerState: LiveData<Result<String>> = _registerState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val name = auth.currentUser?.displayName ?: "User"
                _loginState.value = Result.success(name)
            } catch (e: Exception) {
                _loginState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, address: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid

                db.collection("users").document(uid).set(
                    mapOf("name" to name, "email" to email, "address" to address)
                ).await()

                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(name).build()
                auth.currentUser?.updateProfile(profileUpdate)?.await()

                _registerState.value = Result.success("Registration successful!")
            } catch (e: Exception) {
                _registerState.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isLoggedIn() = SessionManager.isLoggedIn()

    fun logout() = SessionManager.logout()
}