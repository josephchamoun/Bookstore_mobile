package com.example.bookstore.ui.cart

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.viewmodel.CartViewModel
import com.example.bookstore.viewmodel.OrderState

class CartFragment : Fragment() {

    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var adapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cart, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvCart        = view.findViewById<RecyclerView>(R.id.rvCart)
        val tvTotal       = view.findViewById<TextView>(R.id.tvTotal)
        val tvItemCount   = view.findViewById<TextView>(R.id.tvItemCount)
        val btnPlaceOrder = view.findViewById<Button>(R.id.btnPlaceOrder)
        val btnClearCart  = view.findViewById<ImageButton>(R.id.btnClearCart)

        // ← CHANGED: callbacks now pass bookId (String) instead of CartEntity
        adapter = CartAdapter(
            onPlus   = { item -> cartViewModel.updateQuantity(item.bookId, item.quantity + 1) },
            onMinus  = { item -> cartViewModel.updateQuantity(item.bookId, item.quantity - 1) },
            onRemove = { item -> cartViewModel.removeItem(item.bookId) }
        )
        rvCart.layoutManager = LinearLayoutManager(requireContext())
        rvCart.adapter       = adapter

        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            tvItemCount.text = "${items.size} item(s) in cart"
        }

        cartViewModel.cartTotal.observe(viewLifecycleOwner) { total ->
            tvTotal.text = "$${"%.2f".format(total)}"
        }

        // ← CHANGED: removed SavedOffline state — Firestore handles offline automatically
        cartViewModel.orderState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrderState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Order placed successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is OrderState.Error -> {
                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> Unit
            }
        }

        btnClearCart.setOnClickListener {
            cartViewModel.clearCart()
        }

        btnPlaceOrder.setOnClickListener {
            val items = cartViewModel.cartItems.value ?: emptyList()
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddressDialog()
        }

        cartViewModel.loadCart()
    }

    private fun showAddressDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter shipping address"
            setPadding(40, 20, 40, 20)
            // ← CHANGED: removed getSavedAddress() — no longer in CartViewModel
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Shipping Address")
            .setView(input)
            .setPositiveButton("Place Order") { _, _ ->
                val address = input.text.toString().trim()
                if (address.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please enter an address",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    cartViewModel.placeOrder(address)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}