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
import com.example.bookstore.database.ReviewEntity
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter : ListAdapter<ReviewEntity, ReviewAdapter.ViewHolder>(DiffCallback()) {

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

        // First letter of name as avatar
        holder.tvAvatar.text   = review.userName.firstOrNull()?.uppercase() ?: "?"
        holder.tvUserName.text = review.userName
        holder.ratingBar.rating = review.rating.toFloat()
        holder.tvComment.text  = review.comment
        holder.tvDate.text     = formatDate(review.createdAt)
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val input  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            output.format(input.parse(dateStr)!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReviewEntity>() {
        override fun areItemsTheSame(a: ReviewEntity, b: ReviewEntity) = a.reviewId == b.reviewId
        override fun areContentsTheSame(a: ReviewEntity, b: ReviewEntity) = a == b
    }
}