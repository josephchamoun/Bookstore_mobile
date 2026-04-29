package com.example.bookstore.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bookstore.R
import com.example.bookstore.viewmodel.BookSortOption
import com.example.bookstore.viewmodel.BookViewModel

class CatalogFragment : Fragment() {

    private val viewModel: BookViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter
    private lateinit var categoryAdapter: CategoryAdapter

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

        val rvBooks          = view.findViewById<RecyclerView>(R.id.rvBooks)
        val rvCategories     = view.findViewById<RecyclerView>(R.id.rvCategories)
        val etSearch         = view.findViewById<EditText>(R.id.etSearch)
        val etMinPrice       = view.findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice       = view.findViewById<EditText>(R.id.etMaxPrice)
        val cbInStock        = view.findViewById<CheckBox>(R.id.cbInStock)
        val spinnerSort      = view.findViewById<Spinner>(R.id.spinnerSort)
        val btnClearFilters  = view.findViewById<Button>(R.id.btnClearFilters)
        val swipeRefresh     = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val tvCount          = view.findViewById<TextView>(R.id.tvResultCount)
        val btnToggleFilters = view.findViewById<Button>(R.id.btnToggleFilters)
        val filterPanel      = view.findViewById<LinearLayout>(R.id.filterPanel)

        bookAdapter = BookAdapter(
            onClick = { book ->
                startActivity(
                    Intent(requireContext(), BookDetailsActivity::class.java).apply {
                        // ← CHANGED: book.bookId is now String, not Int
                        putExtra("book_id", book.bookId)
                    }
                )
            },
            onFavoriteClick = { book -> viewModel.toggleFavorite(book) }
        )
        rvBooks.layoutManager = GridLayoutManager(requireContext(), 2)
        rvBooks.adapter = bookAdapter

        categoryAdapter = CategoryAdapter { categoryId ->
            viewModel.updateCategory(categoryId)
        }
        rvCategories.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvCategories.adapter = categoryAdapter

        spinnerSort.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            BookSortOption.entries.map { it.label }
        )
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) { viewModel.updateSortOption(BookSortOption.entries[position]) }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { viewModel.updateQuery(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        val priceWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.updatePriceRange(
                    etMinPrice.text.toString(),
                    etMaxPrice.text.toString()
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }
        etMinPrice.addTextChangedListener(priceWatcher)
        etMaxPrice.addTextChangedListener(priceWatcher)

        cbInStock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateInStockOnly(isChecked)
        }

        btnClearFilters.setOnClickListener {
            etSearch.text?.clear()
            etMinPrice.text?.clear()
            etMaxPrice.text?.clear()
            cbInStock.isChecked = false
            spinnerSort.setSelection(BookSortOption.entries.indexOf(BookSortOption.NEWEST))
            categoryAdapter.clearSelection()
            viewModel.clearFilters()
            filterPanel.visibility = View.GONE
            btnToggleFilters.text = "⚙ Filters"
        }

        btnToggleFilters.setOnClickListener {
            if (filterPanel.visibility == View.GONE) {
                filterPanel.visibility = View.VISIBLE
                btnToggleFilters.text = "✕ Hide Filters"
            } else {
                filterPanel.visibility = View.GONE
                btnToggleFilters.text = "⚙ Filters"
            }
        }

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.submitList(books)
            tvCount.text = "Showing ${books.size} books"
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