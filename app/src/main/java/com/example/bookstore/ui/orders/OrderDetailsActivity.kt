package com.example.bookstore.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.model.Order
import com.example.bookstore.viewmodel.OrderViewModel
import com.google.gson.Gson

class OrderDetailsActivity : AppCompatActivity() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var currentOrder: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvOrderId = findViewById<TextView>(R.id.tvOrderId)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val stepPlaced = findViewById<TextView>(R.id.stepPlaced)
        val stepProcessing = findViewById<TextView>(R.id.stepProcessing)
        val stepShipped = findViewById<TextView>(R.id.stepShipped)
        val stepDelivered = findViewById<TextView>(R.id.stepDelivered)
        val btnCancelOrder = findViewById<Button>(R.id.btnCancelOrder)
        val rvItems = findViewById<RecyclerView>(R.id.rvOrderItems)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val orderJson = intent.getStringExtra("order_json") ?: return
        currentOrder = Gson().fromJson(orderJson, Order::class.java)

        rvItems.layoutManager = LinearLayoutManager(this)

        fun render(order: Order) {
            currentOrder = order
            tvOrderId.text = "Order #${order.orderId}"
            tvStatus.text = order.status.replaceFirstChar { it.uppercase() }
            tvDate.text = "Date: ${order.orderDate}"
            tvAddress.text = "Address: ${order.shippingAddress}"
            tvTotal.text = "$${"%.2f".format(order.total)}"
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, statusColor(order.status))
            )
            applyStepStyle(stepPlaced, !order.status.equals("cancelled", true))
            applyStepStyle(stepProcessing, orderReached(order.status, "processing"))
            applyStepStyle(stepShipped, orderReached(order.status, "shipped"))
            applyStepStyle(stepDelivered, orderReached(order.status, "delivered"))
            btnCancelOrder.visibility =
                if (order.status.equals("pending", ignoreCase = true)) View.VISIBLE else View.GONE
            rvItems.adapter = OrderItemAdapter(order.items ?: emptyList())
        }

        btnCancelOrder.setOnClickListener {
            viewModel.cancelOrder(currentOrder.orderId)
        }

        viewModel.orders.observe(this) { orders ->
            orders.firstOrNull { it.orderId == currentOrder.orderId }?.let(::render)
        }

        viewModel.cancelState.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                Toast.makeText(this, result.getOrNull() ?: "Order cancelled", Toast.LENGTH_SHORT).show()
                viewModel.refreshOrders()
            } else {
                Toast.makeText(
                    this,
                    result.exceptionOrNull()?.message ?: "Unable to cancel order",
                    Toast.LENGTH_SHORT
                ).show()
            }
            viewModel.consumeCancelState()
        }

        render(currentOrder)
    }

    private fun orderReached(status: String, step: String): Boolean {
        val orderedStatuses = listOf("pending", "processing", "shipped", "delivered")
        val statusIndex = orderedStatuses.indexOf(status.lowercase())
        val stepIndex = orderedStatuses.indexOf(step.lowercase())
        return statusIndex >= stepIndex && statusIndex >= 0
    }

    private fun applyStepStyle(view: TextView, active: Boolean) {
        view.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, if (active) R.color.accent_blue else R.color.surface2)
        )
        view.alpha = if (active) 1f else 0.65f
    }

    private fun statusColor(status: String): Int = when (status.lowercase()) {
        "pending" -> R.color.accent_blue
        "processing" -> R.color.success
        "shipped" -> android.R.color.holo_orange_dark
        "delivered" -> android.R.color.holo_green_dark
        "cancelled" -> R.color.error
        else -> R.color.text_secondary
    }
}
