package com.example.bookstore.repository

import android.content.Context
import android.util.Log
import com.example.bookstore.database.AppDatabase
import com.example.bookstore.database.CartEntity
import com.example.bookstore.database.OrderEntity
import com.example.bookstore.database.PendingOrderEntity
import com.example.bookstore.model.Order
import com.example.bookstore.model.OrderItem
import com.example.bookstore.model.PlaceOrderResponse
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class OrderRepository(context: Context, private val sessionManager: SessionManager) {

    private val api             = RetrofitClient.instance
    private val orderDao        = AppDatabase.getInstance(context).orderDao()
    private val pendingOrderDao = AppDatabase.getInstance(context).pendingOrderDao()
    private val gson            = Gson()

    private data class PendingOrderItem(
        val book_id: Int,
        val quantity: Int
    )

    // ── Reactive orders stream (replaces the one-shot getOrders) ──────────

    fun observeOrders(): Flow<List<Order>> =
        orderDao.getAllOrders().map { entities -> entities.map { it.toOrder() } }

    // ── Network refresh (called by WorkManager or pull-to-refresh) ────────

    suspend fun refreshOrders(): List<Order> = fetchAndCacheOrders()

    private suspend fun fetchAndCacheOrders(): List<Order> {
        return try {
            val response = api.getOrders(sessionManager.getBearerToken())
            if (response.isSuccessful) {
                val orders = response.body()?.orders ?: emptyList()
                val now    = Instant.now().toString()
                // Upsert fresh orders — no clearAll() so cache survives failed calls
                orderDao.upsertAll(orders.map { it.toEntity(now) })
                // Remove orders that no longer exist on the server
                if (orders.isNotEmpty()) {
                    orderDao.deleteAbsent(orders.map { it.orderId })
                }
                orders
            } else emptyList()
        } catch (e: Exception) {
            // Network unavailable — cached orders stay intact, Flow keeps last emission
            emptyList()
        }
    }

    // ── Place order (online) ───────────────────────────────────────────────

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
                Result.failure(Exception("Order failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Offline queue ─────────────────────────────────────────────────────

    suspend fun saveOffline(
        cartItems: List<CartEntity>,
        shippingAddress: String,
        total: Double
    ) {
        pendingOrderDao.insert(
            PendingOrderEntity(
                shippingAddress = shippingAddress,
                totalAmount     = total,
                itemsJson       = gson.toJson(
                    cartItems.map { PendingOrderItem(book_id = it.bookId, quantity = it.quantity) }
                )
            )
        )
    }

    // Called by OrderSyncWorker for each pending row
    suspend fun submitPendingOrder(pending: PendingOrderEntity) {
        val type = object : com.google.gson.reflect.TypeToken<List<PendingOrderItem>>() {}.type
        val items: List<PendingOrderItem> = gson.fromJson(pending.itemsJson, type)

        val body = mapOf(
            "shipping_address" to pending.shippingAddress,
            "items" to items.map {
                mapOf(
                    "book_id" to it.book_id,
                    "quantity" to it.quantity
                )
            }
        )
        val response = api.placeOrder(sessionManager.getBearerToken(), body)
        if (!response.isSuccessful) {
            throw Exception("Sync failed: ${response.code()}")
        }
    }

    suspend fun syncOrdersCache() {
        fetchAndCacheOrders()
    }

    suspend fun cancelOrder(orderId: Int): Result<String> {
        return try {
            val response = api.cancelOrder(
                sessionManager.getBearerToken(),
                mapOf("order_id" to orderId)
            )
            if (response.isSuccessful) {
                orderDao.updateStatus(orderId, "cancelled")
                fetchAndCacheOrders()
                Result.success(response.body()?.message ?: "Order cancelled")
            } else {
                Result.failure(Exception("Cancel failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────

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
        items           = gson.fromJson(
            itemsJson,
            Array<OrderItem>::class.java
        )?.toList()
    )
}
