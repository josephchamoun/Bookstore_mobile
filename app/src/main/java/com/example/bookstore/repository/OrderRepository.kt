package com.example.bookstore.repository

import android.content.Context
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.database.CartEntity
import com.example.bookstore.database.OrderEntity
import com.example.bookstore.model.Order
import com.example.bookstore.model.PlaceOrderResponse
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.google.gson.Gson
import java.time.Instant

class OrderRepository(context: Context, private val sessionManager: SessionManager) {

    private val api      = RetrofitClient.instance
    private val orderDao = AppDatabase.getInstance(context).orderDao()
    private val gson     = Gson()

    suspend fun getOrders(): List<Order> {
        // Return cache first
        val cached = orderDao.getAllOrders()
        if (cached.isNotEmpty()) return cached.map { it.toOrder() }
        return fetchAndCacheOrders()
    }

    suspend fun refreshOrders(): List<Order> = fetchAndCacheOrders()

    private suspend fun fetchAndCacheOrders(): List<Order> {
        return try {
            val response = api.getOrders(sessionManager.getBearerToken())
            if (response.isSuccessful) {
                val orders = response.body()?.orders ?: emptyList()
                val now    = Instant.now().toString()
                orderDao.clearAll()
                orderDao.insertAll(orders.map { it.toEntity(now) })
                orders
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun placeOrder(
        cartItems: List<CartEntity>,
        shippingAddress: String
    ): Result<PlaceOrderResponse> {
        return try {
            val items = cartItems.map {
                mapOf("book_id" to it.bookId, "quantity" to it.quantity)
            }
            val body = mapOf(
                "shipping_address" to shippingAddress,
                "items"            to items
            )
            val response = api.placeOrder(sessionManager.getBearerToken(), body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Order failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mappers ────────────────────────────────────────────

    private fun Order.toEntity(cachedAt: String) = OrderEntity(
        orderId         = orderId,
        orderDate       = orderDate,
        total           = total,
        status          = status,
        shippingAddress = shippingAddress,
        itemsJson       = gson.toJson(items),
        cachedAt        = cachedAt
    )

    private fun OrderEntity.toOrder() = Order(
        orderId         = orderId,
        userId          = 0,
        orderDate       = orderDate,
        total           = total,
        status          = status,
        shippingAddress = shippingAddress,
        items           = gson.fromJson(itemsJson, Array<com.example.bookstore.model.OrderItem>::class.java)?.toList()
    )
}