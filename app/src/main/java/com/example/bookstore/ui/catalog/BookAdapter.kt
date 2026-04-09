package com.example.bookstore.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstore.R
import com.example.bookstore.model.Book

class BookAdapter(
    private val onClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(DiffCallback) {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover:   ImageView = view.findViewById(R.id.ivCover)
        val tvTitle:   TextView  = view.findViewById(R.id.tvTitle)
        val tvAuthor:  TextView  = view.findViewById(R.id.tvAuthor)
        val tvPrice:   TextView  = view.findViewById(R.id.tvPrice)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.tvTitle.text    = book.title
        holder.tvAuthor.text   = book.author
        holder.tvPrice.text    = "$${book.price}"
        holder.tvCategory.text = book.categoryName ?: ""

        Glide.with(holder.itemView.context)
            .load(book.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(book) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(a: Book, b: Book) = a.bookId == b.bookId
        override fun areContentsTheSame(a: Book, b: Book) = a == b
    }
}