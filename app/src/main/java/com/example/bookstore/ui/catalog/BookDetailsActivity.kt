package com.example.bookstore.ui.catalog

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.bookstore.R
import com.example.bookstore.ui.reviews.ReviewsActivity
import com.example.bookstore.viewmodel.BookViewModel
import com.example.bookstore.viewmodel.CartViewModel
import com.example.bookstore.viewmodel.ReviewViewModel

class BookDetailsActivity : AppCompatActivity() {

    private val bookViewModel   : BookViewModel   by viewModels()
    private val cartViewModel   : CartViewModel   by viewModels()
    private val reviewViewModel : ReviewViewModel by viewModels()

    private var bookId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_details)

        val toolbar        = findViewById<Toolbar>(R.id.toolbar)
        val ivCover        = findViewById<ImageView>(R.id.ivCover)
        val tvTitle        = findViewById<TextView>(R.id.tvTitle)
        val tvAuthor       = findViewById<TextView>(R.id.tvAuthor)
        val tvCategory     = findViewById<TextView>(R.id.tvCategory)
        val tvStock        = findViewById<TextView>(R.id.tvStock)
        val tvPrice        = findViewById<TextView>(R.id.tvPrice)
        val btnFavorite    = findViewById<ImageButton>(R.id.btnFavorite)
        val btnAddToCart   = findViewById<Button>(R.id.btnAddToCart)
        val btnViewReviews = findViewById<Button>(R.id.btnViewReviews)
        val ratingBarBook  = findViewById<RatingBar>(R.id.ratingBarBook)
        val tvBookRating   = findViewById<TextView>(R.id.tvBookRating)
        val progressBar    = findViewById<ProgressBar>(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        toolbar.setNavigationOnClickListener { finish() }

        bookId = intent.getIntExtra("book_id", -1)
        if (bookId == -1) { finish(); return }

        // ── Book data ─────────────────────────────────────────────────────────
        bookViewModel.loadBookById(bookId)

        bookViewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        bookViewModel.selectedBook.observe(this) { book ->
            if (book == null) return@observe

            tvTitle.text    = book.title
            tvAuthor.text   = "by ${book.author}"
            tvCategory.text = book.categoryName ?: ""
            tvPrice.text    = "$${"%.2f".format(book.price)}"
            tvStock.text    = if (book.stock > 0) "In stock: ${book.stock}" else "Out of stock"

            btnFavorite.setImageResource(
                if (book.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_outline
            )

            Glide.with(this)
                .load(book.coverUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivCover)

            btnFavorite.setOnClickListener { bookViewModel.toggleFavorite(book) }

            btnAddToCart.setOnClickListener { cartViewModel.addToCart(book) }

            // Open ReviewsActivity with book title for toolbar
            btnViewReviews.setOnClickListener {
                ReviewsActivity.start(this, bookId, book.title)
            }
        }

        // ── Reviews rating ────────────────────────────────────────────────────
        reviewViewModel.init(bookId)

        reviewViewModel.averageRating.observe(this) { avg ->
            ratingBarBook.rating = avg
            tvBookRating.text = if (avg > 0f) "%.1f / 5.0".format(avg)
            else "No ratings yet"
        }

        // ── Cart feedback ─────────────────────────────────────────────────────
        cartViewModel.message.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                cartViewModel.clearMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (bookId != -1) reviewViewModel.refreshReviews(bookId)
    }
}