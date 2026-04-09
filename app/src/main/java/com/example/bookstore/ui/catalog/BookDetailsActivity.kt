package com.example.bookstore.ui.catalog

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.bookstore.R
import com.example.bookstore.viewmodel.BookViewModel
import com.example.bookstore.viewmodel.CartViewModel

class BookDetailsActivity : AppCompatActivity() {

    private val bookViewModel: BookViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_details)

        val toolbar      = findViewById<Toolbar>(R.id.toolbar)
        val ivCover      = findViewById<ImageView>(R.id.ivCover)
        val tvTitle      = findViewById<TextView>(R.id.tvTitle)
        val tvAuthor     = findViewById<TextView>(R.id.tvAuthor)
        val tvCategory   = findViewById<TextView>(R.id.tvCategory)
        val tvStock      = findViewById<TextView>(R.id.tvStock)
        val tvPrice      = findViewById<TextView>(R.id.tvPrice)
        val btnAddToCart = findViewById<Button>(R.id.btnAddToCart)
        val progressBar  = findViewById<ProgressBar>(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val bookId = intent.getIntExtra("book_id", -1)
        if (bookId == -1) { finish(); return }

        bookViewModel.loadBookById(bookId)

        bookViewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        bookViewModel.selectedBook.observe(this) { book ->
            if (book == null) return@observe
            tvTitle.text    = book.title
            tvAuthor.text   = "by ${book.author}"
            tvCategory.text = book.categoryName ?: ""
            tvPrice.text    = "$${book.price}"
            tvStock.text    = if (book.stock > 0) "✓  In Stock" else "✗  Out of Stock"

            Glide.with(this)
                .load(book.coverUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivCover)

            btnAddToCart.setOnClickListener {
                cartViewModel.addToCart(book)
            }
        }

        cartViewModel.message.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                cartViewModel.clearMessage()
            }
        }
    }
}