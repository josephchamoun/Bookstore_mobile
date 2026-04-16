package com.example.bookstore.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.ui.auth.LoginActivity
import com.example.bookstore.ui.catalog.BookAdapter
import com.example.bookstore.ui.catalog.BookDetailsActivity
import com.example.bookstore.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var favoritesAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onResume() {
        super.onResume()
        viewModel.refreshProfile()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvUserId = view.findViewById<TextView>(R.id.tvUserId)
        val etAddress = view.findViewById<EditText>(R.id.etSavedAddress)
        val btnSaveAddress = view.findViewById<Button>(R.id.btnSaveAddress)
        val btnLogout = view.findViewById<Button>(R.id.btnLogoutProfile)
        val tvFavorites = view.findViewById<TextView>(R.id.tvFavoriteCount)
        val tvOrders = view.findViewById<TextView>(R.id.tvOrderCount)
        val tvPending = view.findViewById<TextView>(R.id.tvPendingCount)
        val rvFavorites = view.findViewById<RecyclerView>(R.id.rvFavorites)

        favoritesAdapter = BookAdapter(
            onClick = { book ->
                startActivity(Intent(requireContext(), BookDetailsActivity::class.java).apply {
                    putExtra("book_id", book.bookId)
                })
            },
            onFavoriteClick = { book ->
                viewModel.toggleFavorite(book)
            }
        )
        rvFavorites.layoutManager = GridLayoutManager(requireContext(), 2)
        rvFavorites.adapter = favoritesAdapter

        viewModel.userName.observe(viewLifecycleOwner) { tvName.text = it.ifBlank { "Reader" } }
        viewModel.userId.observe(viewLifecycleOwner) {
            tvUserId.text = if (it.isBlank()) "Guest account" else "User ID #$it"
        }
        viewModel.savedAddress.observe(viewLifecycleOwner) {
            if (etAddress.text.toString() != it) {
                etAddress.setText(it)
            }
        }
        viewModel.favoriteCount.observe(viewLifecycleOwner) { tvFavorites.text = it }
        viewModel.orderCount.observe(viewLifecycleOwner) { tvOrders.text = it }
        viewModel.pendingCount.observe(viewLifecycleOwner) { tvPending.text = it }
        viewModel.favoriteBooks.observe(viewLifecycleOwner) { favoritesAdapter.submitList(it) }

        btnSaveAddress.setOnClickListener {
            viewModel.saveAddress(etAddress.text.toString().trim())
            Toast.makeText(requireContext(), "Address saved", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }
}
