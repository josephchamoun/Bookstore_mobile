package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.network.SessionManager
import com.example.bookstore.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository     = AuthRepository(sessionManager)

    private val _loginState  = MutableLiveData<Result<String>>()
    val loginState: LiveData<Result<String>> = _loginState

    private val _registerState = MutableLiveData<Result<String>>()
    val registerState: LiveData<Result<String>> = _registerState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.login(email, password)
            if (result.isSuccess) {
                val name = sessionManager.getUserName() ?: "User"
                _loginState.value = Result.success(name)
            } else {
                _loginState.value = Result.failure(result.exceptionOrNull()!!)
            }
            _isLoading.value = false
        }
    }

    fun register(name: String, email: String, password: String, address: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.register(name, email, password, address)
            if (result.isSuccess) {
                _registerState.value = Result.success(result.getOrNull()?.message ?: "Success")
            } else {
                _registerState.value = Result.failure(result.exceptionOrNull()!!)
            }
            _isLoading.value = false
        }
    }

    fun isLoggedIn() = sessionManager.isLoggedIn()

    fun logout() = sessionManager.clearSession()
}