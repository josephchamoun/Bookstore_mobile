package com.example.bookstore.ui.orders

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.model.Order
import com.google.gson.Gson

class OrderAdapter : ListAdapter<Order, OrderAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId:    TextView = view.findViewById(R.id.tvOrderId)
        val tvStatus:     TextView = view.findViewById(R.id.tvStatus)
        val tvDate:       TextView = view.findViewById(R.id.tvDate)
        val tvTotal:      TextView = view.findViewById(R.id.tvTotal)
        val tvItems:      TextView = view.findViewById(R.id.tvItems)
        // ← CHANGED: tvPendingSync kept but always hidden now
        val tvPendingSync: TextView = view.findViewById(R.id.tvPendingSync)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = getItem(position)

        // ← CHANGED: orderId is String — show first 8 chars to keep it short
        holder.tvOrderId.text = "Order #${order.orderId.take(8)}"
        holder.tvDate.text = order.orderDate?.toDate()?.let {
            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(it)
        } ?: ""
        holder.tvTotal.text   = "$${"%.2f".format(order.total)}"
        holder.tvItems.text   = "${order.items.size} item(s)"

        // ← CHANGED: no more isSynced — all orders from Firestore are synced
        holder.tvPendingSync.visibility = View.GONE
        holder.tvStatus.text = order.status.replaceFirstChar { it.uppercase() }

        holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(holder.itemView.context, statusColor(order.status))
        )

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, OrderDetailsActivity::class.java)
            intent.putExtra("order_json", Gson().toJson(order))
            holder.itemView.context.startActivity(intent)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order)     = a.orderId == b.orderId
        override fun areContentsTheSame(a: Order, b: Order)  = a == b
    }

    private fun statusColor(status: String): Int = when (status.lowercase()) {
        "pending"    -> R.color.accent_blue
        "processing" -> R.color.success
        "shipped"    -> android.R.color.holo_orange_dark
        "delivered"  -> android.R.color.holo_green_dark
        "cancelled"  -> R.color.error
        else         -> R.color.text_secondary
    }
}