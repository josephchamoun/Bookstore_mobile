package com.example.bookstore.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bookstore.R
import com.example.bookstore.viewmodel.BookViewModel

class CatalogFragment : Fragment() {

    private val viewModel: BookViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategoryId: Int? = null
    override fun onResume() {
        super.onResume()
        Log.d("CatalogFragment", "onResume called")
        viewModel.refresh()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_catalog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvBooks      = view.findViewById<RecyclerView>(R.id.rvBooks)
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        val etSearch     = view.findViewById<EditText>(R.id.etSearch)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvCount      = view.findViewById<TextView>(R.id.tvResultCount)

        // Books grid
        bookAdapter = BookAdapter { book ->
            val intent = Intent(requireContext(), BookDetailsActivity::class.java)
            intent.putExtra("book_id", book.bookId)
            startActivity(intent)
        }
        rvBooks.layoutManager = GridLayoutManager(requireContext(), 2)
        rvBooks.adapter       = bookAdapter

        // Categories horizontal list
        categoryAdapter = CategoryAdapter { categoryId ->
            selectedCategoryId = categoryId
            categoryId?.let {
                viewModel.filterByCategory(it).observe(viewLifecycleOwner) { books ->
                    bookAdapter.submitList(books)
                    tvCount.text = "Showing ${books.size} books"
                }
            }
        }
        rvCategories.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        rvCategories.adapter = categoryAdapter

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    // Empty search — fall back to the full reactive stream
                    selectedCategoryId = null
                    viewModel.books.observe(viewLifecycleOwner) { books ->
                        bookAdapter.submitList(books)
                        tvCount.text = "Showing ${books.size} books"
                        swipeRefresh.isRefreshing = false
                    }
                } else if (query.length >= 2) {
                    viewModel.searchBooks(query).observe(viewLifecycleOwner) { books ->
                        bookAdapter.submitList(books)
                        tvCount.text = "Showing ${books.size} books"
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Pull to refresh — triggers network fetch, Flow re-emits automatically
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // Observe full reactive book stream (default state — no search/filter active)
        viewModel.books.observe(viewLifecycleOwner) { books ->
            // Only update list if no search/filter is active
            if (etSearch.text.isNullOrEmpty() && selectedCategoryId == null) {
                bookAdapter.submitList(books)
                tvCount.text = "Showing ${books.size} books"
            }
            swipeRefresh.isRefreshing = false
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            swipeRefresh.isRefreshing = loading
        }
    }
}