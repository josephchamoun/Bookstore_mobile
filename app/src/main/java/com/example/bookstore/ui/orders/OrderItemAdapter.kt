package com.example.bookstore.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookstore.R
import com.example.bookstore.model.OrderItem

class OrderItemAdapter(
    private val items: List<OrderItem>
) : RecyclerView.Adapter<OrderItemAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover:    ImageView = view.findViewById(R.id.ivCover)
        val tvTitle:    TextView  = view.findViewById(R.id.tvTitle)
        val tvAuthor:   TextView  = view.findViewById(R.id.tvAuthor)
        val tvQuantity: TextView  = view.findViewById(R.id.tvQuantity)
        val tvPrice:    TextView  = view.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text    = item.bookTitle ?: "Unknown Book"
        holder.tvAuthor.text   = item.bookAuthor ?: ""
        holder.tvQuantity.text = "Qty: ${item.quantity}"
        holder.tvPrice.text    = "$${"%.2f".format(item.unitPrice * item.quantity)}"

        Glide.with(holder.itemView.context)
            .load(item.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivCover)
    }

    override fun getItemCount() = items.size
}