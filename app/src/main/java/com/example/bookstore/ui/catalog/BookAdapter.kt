package com.example.bookstore.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstore.R
import com.example.bookstore.model.Book

class BookAdapter(
    private val onClick: (Book) -> Unit,
    private val onFavoriteClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(DiffCallback) {

    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvStock: TextView = view.findViewById(R.id.tvStock)
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
        holder.tvPrice.text    = "$${"%.2f".format(book.price)}"
        holder.tvCategory.text = book.categoryName ?: ""
        holder.tvStock.text    = if (book.stock > 0) "${book.stock} in stock" else "Out of stock"
        holder.btnFavorite.setImageResource(
            if (book.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        )

        Glide.with(holder.itemView.context)
            .load(book.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(book) }
        holder.btnFavorite.setOnClickListener { onFavoriteClick(book) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(a: Book, b: Book) = a.bookId == b.bookId
        override fun areContentsTheSame(a: Book, b: Book) = a == b
    }
}
