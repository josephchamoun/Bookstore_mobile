package com.example.bookstore.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.model.Order

class OrderAdapter : ListAdapter<Order, OrderAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvStatus:  TextView = view.findViewById(R.id.tvStatus)
        val tvDate:    TextView = view.findViewById(R.id.tvDate)
        val tvTotal:   TextView = view.findViewById(R.id.tvTotal)
        val tvItems:   TextView = view.findViewById(R.id.tvItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = getItem(position)
        holder.tvOrderId.text = "Order #${order.orderId}"
        holder.tvStatus.text  = order.status.replaceFirstChar { it.uppercase() }
        holder.tvDate.text    = order.orderDate
        holder.tvTotal.text   = "$${"%.2f".format(order.total)}"
        holder.tvItems.text   = "${order.items?.size ?: 0} item(s)"


        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(
                holder.itemView.context,
                OrderDetailsActivity::class.java
            )
            intent.putExtra("order_json", com.google.gson.Gson().toJson(order))
            holder.itemView.context.startActivity(intent)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.orderId == b.orderId
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}