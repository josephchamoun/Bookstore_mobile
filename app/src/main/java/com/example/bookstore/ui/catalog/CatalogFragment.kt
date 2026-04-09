package com.example.bookstore.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
            viewModel.loadBooks(categoryId = categoryId)
        }
        rvCategories.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        rvCategories.adapter = categoryAdapter

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length >= 2 || query.isEmpty()) {
                    viewModel.loadBooks(search = query.ifEmpty { null })
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Pull to refresh
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshBooks()
            viewModel.refreshCategories()
        }

        // Observe
        viewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.submitList(books)
            tvCount.text = "Showing ${books.size} books"
            swipeRefresh.isRefreshing = false
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }

        // Load data
        viewModel.loadBooks()
        viewModel.loadCategories()
    }
}