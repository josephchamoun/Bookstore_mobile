package com.example.bookstore.ui.orders

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.example.bookstore.R
import com.example.bookstore.model.Order
import com.google.gson.Gson

class OrderDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        val toolbar    = findViewById<Toolbar>(R.id.toolbar)
        val tvOrderId  = findViewById<TextView>(R.id.tvOrderId)
        val tvStatus   = findViewById<TextView>(R.id.tvStatus)
        val tvDate     = findViewById<TextView>(R.id.tvDate)
        val tvAddress  = findViewById<TextView>(R.id.tvAddress)
        val tvTotal    = findViewById<TextView>(R.id.tvTotal)
        val rvItems    = findViewById<RecyclerView>(R.id.rvOrderItems)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Get order passed as JSON string
        val orderJson = intent.getStringExtra("order_json") ?: return
        val order     = Gson().fromJson(orderJson, Order::class.java)

        tvOrderId.text = "Order #${order.orderId}"
        tvStatus.text  = order.status.replaceFirstChar { it.uppercase() }
        tvDate.text    = "📅 ${order.orderDate}"
        tvAddress.text = "📍 ${order.shippingAddress}"
        tvTotal.text   = "$${"%.2f".format(order.total)}"

        val items = order.items ?: emptyList()
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter       = OrderItemAdapter(items)
    }
}