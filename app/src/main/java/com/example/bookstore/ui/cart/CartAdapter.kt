package com.example.bookstore.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstore.R
import com.example.bookstore.repository.CartRepository

class CartAdapter(
    private val onPlus:   (CartRepository.CartItem) -> Unit,
    private val onMinus:  (CartRepository.CartItem) -> Unit,
    private val onRemove: (CartRepository.CartItem) -> Unit
) : ListAdapter<CartRepository.CartItem, CartAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover:    ImageView = view.findViewById(R.id.ivCover)
        val tvTitle:    TextView  = view.findViewById(R.id.tvTitle)
        val tvAuthor:   TextView  = view.findViewById(R.id.tvAuthor)
        val tvPrice:    TextView  = view.findViewById(R.id.tvPrice)
        val tvQuantity: TextView  = view.findViewById(R.id.tvQuantity)
        val btnPlus:    Button    = view.findViewById(R.id.btnPlus)
        val btnMinus:   Button    = view.findViewById(R.id.btnMinus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvTitle.text    = item.title
        holder.tvAuthor.text   = item.author
        holder.tvPrice.text    = "$${item.unitPrice}"
        holder.tvQuantity.text = item.quantity.toString()

        Glide.with(holder.itemView.context)
            .load(item.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivCover)

        holder.btnPlus.setOnClickListener  { onPlus(item) }
        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) onMinus(item) else onRemove(item)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CartRepository.CartItem>() {
        override fun areItemsTheSame(a: CartRepository.CartItem, b: CartRepository.CartItem) =
            a.bookId == b.bookId
        override fun areContentsTheSame(a: CartRepository.CartItem, b: CartRepository.CartItem) =
            a == b
    }
}