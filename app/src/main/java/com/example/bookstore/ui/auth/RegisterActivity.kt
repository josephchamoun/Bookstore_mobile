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
import com.example.bookstore.R
import com.example.bookstore.viewmodel.AuthViewModel

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName      = findViewById<EditText>(R.id.etName)
        val etEmail     = findViewById<EditText>(R.id.etEmail)
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val etAddress   = findViewById<EditText>(R.id.etAddress)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin     = findViewById<TextView>(R.id.tvLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnRegister.setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val address  = etAddress.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(name, email, password, address)
        }

        tvLogin.setOnClickListener { finish() }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnRegister.isEnabled  = !loading
        }

        viewModel.registerState.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_LONG).show()
                finish()
            }
            result.onFailure {
                Toast.makeText(this, "Registration failed. Email may already be used.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}