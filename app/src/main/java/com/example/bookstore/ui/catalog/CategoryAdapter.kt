package com.example.bookstore.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.model.Category

class CategoryAdapter(
    private val onSelect: (Int?) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DiffCallback) {

    private var selectedId: Int? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.tvName.text = category.name

        val isSelected = category.categoryId == selectedId
        holder.tvName.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip else R.drawable.bg_chip_dark
        )

        holder.itemView.setOnClickListener {
            val prev = selectedId
            selectedId = if (prev == category.categoryId) null else category.categoryId
            notifyItemChanged(position)
            if (prev != null) {
                val prevPos = currentList.indexOfFirst { it.categoryId == prev }
                if (prevPos >= 0) notifyItemChanged(prevPos)
            }
            onSelect(selectedId)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(a: Category, b: Category) = a.categoryId == b.categoryId
        override fun areContentsTheSame(a: Category, b: Category) = a == b
    }

    fun clearSelection() {
        val previous = selectedId
        selectedId = null
        if (previous != null) {
            val previousIndex = currentList.indexOfFirst { it.categoryId == previous }
            if (previousIndex >= 0) notifyItemChanged(previousIndex)
        }
    }
}
