package com.example.bookstore.repository

import com.example.bookstore.auth.SessionManager
import com.example.bookstore.model.Order
import com.example.bookstore.model.OrderItem
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val db = Firebase.firestore

    // ── Reactive orders stream ────────────────────────────────────────────
    fun getOrders(): Flow<List<Order>> = callbackFlow {
        val userId = SessionManager.getCurrentUserId()
        val listener = db.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val orders = snapshot?.documents?.map { doc ->
                    val rawItems = doc.get("items") as? List<*> ?: emptyList<Any>()
                    val items = rawItems.mapNotNull { raw ->
                        val map = raw as? Map<*, *> ?: return@mapNotNull null
                        OrderItem(
                            bookId    = map["bookId"].toString(),
                            bookTitle = map["bookTitle"].toString(),
                            bookAuthor = map["bookAuthor"].toString(),
                            coverUrl  = map["coverUrl"].toString(),
                            quantity  = (map["quantity"] as? Long)?.toInt() ?: 0,
                            unitPrice = (map["unitPrice"] as? Double) ?: 0.0
                        )
                    }
                    Order(
                        orderId         = doc.id,
                        userId          = doc.getString("userId") ?: "",
                        orderDate       = doc.getTimestamp("orderDate"),
                        total           = doc.getDouble("total") ?: 0.0,
                        status          = doc.getString("status") ?: "pending",
                        shippingAddress = doc.getString("shippingAddress") ?: "",
                        items           = items
                    )
                } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    // ── Place order (offline-safe — Firestore queues it automatically) ────
    suspend fun placeOrder(
        cartItems: List<CartRepository.CartItem>,
        shippingAddress: String
    ): Result<String> {
        return try {
            val userId = SessionManager.getCurrentUserId()
            val total = cartItems.sumOf { it.unitPrice * it.quantity }
            val order = hashMapOf(
                "userId"          to userId,
                "orderDate" to com.google.firebase.Timestamp.now(),
                "total"           to total,
                "status"          to "pending",
                "shippingAddress" to shippingAddress,
                "items"           to cartItems.map { item ->
                    hashMapOf(
                        "bookId"     to item.bookId,
                        "bookTitle"  to item.title,
                        "bookAuthor" to item.author,
                        "coverUrl"   to item.coverUrl,
                        "quantity"   to item.quantity,
                        "unitPrice"  to item.unitPrice
                    )
                }
            )
            val ref = db.collection("orders").add(order).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Cancel order ──────────────────────────────────────────────────────
    suspend fun cancelOrder(orderId: String): Result<String> {
        return try {
            db.collection("orders").document(orderId)
                .update("status", "cancelled").await()
            Result.success("Order cancelled")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}