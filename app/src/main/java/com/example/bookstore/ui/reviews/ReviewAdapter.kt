package com.example.bookstore.ui.reviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.model.Review  // ← CHANGED: ReviewEntity → Review
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar   : TextView  = view.findViewById(R.id.tvAvatar)
        val tvUserName : TextView  = view.findViewById(R.id.tvUserName)
        val tvDate     : TextView  = view.findViewById(R.id.tvDate)
        val ratingBar  : RatingBar = view.findViewById(R.id.ratingBar)
        val tvComment  : TextView  = view.findViewById(R.id.tvComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = getItem(position)
        holder.tvAvatar.text    = review.userName.firstOrNull()?.uppercase() ?: "?"
        holder.tvUserName.text  = review.userName
        holder.ratingBar.rating = review.rating.toFloat()
        holder.tvComment.text   = review.comment
        // ← CHANGED: format Timestamp instead of String date
        holder.tvDate.text      = formatTimestamp(review.createdAt)
    }

    // ← CHANGED: accepts Timestamp? instead of String
    private fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        return try {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(timestamp.toDate())
        } catch (e: Exception) {
            ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(a: Review, b: Review) = a.reviewId == b.reviewId
        override fun areContentsTheSame(a: Review, b: Review) = a == b
    }
}