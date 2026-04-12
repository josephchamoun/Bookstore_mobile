package com.example.bookstore.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bookstore.R
import com.example.bookstore.viewmodel.OrderViewModel

class OrdersFragment : Fragment() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var adapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_orders, container, false)

    override fun onResume() {
        super.onResume()
        viewModel.refreshOrders()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvOrders     = view.findViewById<RecyclerView>(R.id.rvOrders)
        val progressBar  = view.findViewById<ProgressBar>(R.id.progressBar)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        adapter = OrderAdapter()
        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        rvOrders.adapter       = adapter

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility    = if (loading) View.VISIBLE else View.GONE
            swipeRefresh.isRefreshing = loading
        }

        viewModel.orders.observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.refreshOrders()
        }
    }
}