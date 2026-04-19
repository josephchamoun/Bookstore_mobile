package com.example.bookstore.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bookstore.MainActivity
import com.example.bookstore.R
import com.example.bookstore.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip login if already logged in
        val vm: AuthViewModel by viewModels()
        if (vm.isLoggedIn()) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail     = findViewById<EditText>(R.id.etEmail)
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val btnLogin    = findViewById<Button>(R.id.btnLogin)
        val tvRegister  = findViewById<TextView>(R.id.tvRegister)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnLogin.isEnabled     = !loading
        }

        viewModel.loginState.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                goToMain()
            }
            result.onFailure { error ->
                val message = error.message ?: ""
                if (message.startsWith("banned:")) {
                    val reason = message.removePrefix("banned:")
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Account Banned")
                        .setMessage(reason)
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}