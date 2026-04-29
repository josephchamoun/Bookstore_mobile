package com.example.bookstore.ui.reviews

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bookstore.R
import com.example.bookstore.viewmodel.ReviewViewModel

class ReviewsActivity : AppCompatActivity() {

    private val viewModel: ReviewViewModel by viewModels()
    private lateinit var adapter: ReviewAdapter
    private var bookId: String = ""  // ← CHANGED: Int → String

    companion object {
        private const val EXTRA_BOOK_ID    = "book_id"
        private const val EXTRA_BOOK_TITLE = "book_title"

        // ← CHANGED: bookId: Int → String
        fun start(context: Context, bookId: String, bookTitle: String) {
            context.startActivity(
                Intent(context, ReviewsActivity::class.java).apply {
                    putExtra(EXTRA_BOOK_ID, bookId)
                    putExtra(EXTRA_BOOK_TITLE, bookTitle)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviews)

        // ← CHANGED: getStringExtra instead of getIntExtra
        bookId = intent.getStringExtra(EXTRA_BOOK_ID) ?: ""
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Reviews"

        if (bookId.isEmpty()) { finish(); return }

        val toolbar          = findViewById<Toolbar>(R.id.toolbar)
        val tvAverageRating  = findViewById<TextView>(R.id.tvAverageRating)
        val ratingBarAverage = findViewById<RatingBar>(R.id.ratingBarAverage)
        val tvReviewCount    = findViewById<TextView>(R.id.tvReviewCount)
        val swipeRefresh     = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val rvReviews        = findViewById<RecyclerView>(R.id.rvReviews)
        val btnWriteReview   = findViewById<Button>(R.id.btnWriteReview)

        setSupportActionBar(toolbar)
        supportActionBar?.title = bookTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        adapter = ReviewAdapter()
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = adapter

        viewModel.reviews.observe(this) { reviews ->
            adapter.submitList(reviews)
            tvReviewCount.text = when {
                reviews.isEmpty() -> "No reviews yet"
                reviews.size == 1 -> "1 review"
                else              -> "${reviews.size} reviews"
            }
        }

        viewModel.averageRating.observe(this) { avg ->
            tvAverageRating.text    = String.format("%.1f", avg)
            ratingBarAverage.rating = avg
        }

        viewModel.isLoading.observe(this) { loading ->
            swipeRefresh.isRefreshing = loading
        }

        viewModel.isEligible.observe(this) { eligible ->
            btnWriteReview.visibility = if (eligible) View.VISIBLE else View.GONE
        }

        btnWriteReview.setOnClickListener {
            // ← CHANGED: passes String bookId
            WriteReviewBottomSheet.newInstance(bookId)
                .show(supportFragmentManager, "WriteReviewBottomSheet")
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.init(bookId)
        }

        viewModel.init(bookId)
    }

    override fun onResume() {
        super.onResume()
        if (bookId.isNotEmpty()) viewModel.init(bookId)
    }
}