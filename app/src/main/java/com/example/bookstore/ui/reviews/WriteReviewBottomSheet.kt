package com.example.bookstore.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.bookstore.R
import com.example.bookstore.viewmodel.ReviewState
import com.example.bookstore.viewmodel.ReviewViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WriteReviewBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: ReviewViewModel by activityViewModels()
    private var bookId: String = ""  // ← CHANGED: Int → String

    companion object {
        private const val ARG_BOOK_ID = "book_id"

        // ← CHANGED: bookId: Int → String
        fun newInstance(bookId: String): WriteReviewBottomSheet {
            return WriteReviewBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOK_ID, bookId)  // ← CHANGED: putInt → putString
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ← CHANGED: getString instead of getInt
        bookId = arguments?.getString(ARG_BOOK_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_write_review, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBarInput)
        val etComment = view.findViewById<EditText>(R.id.etComment)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        btnCancel.setOnClickListener { dismiss() }

        btnSubmit.setOnClickListener {
            val rating  = ratingBar.rating.toInt()
            val comment = etComment.text.toString().trim()
            viewModel.submitReview(bookId, rating, comment)
        }

        viewModel.reviewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ReviewState.Loading -> {
                    btnSubmit.isEnabled = false
                    btnSubmit.text      = "Submitting..."
                }
                is ReviewState.Success -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                    dismiss()
                }
                // ← CHANGED: removed SavedOffline state — Firestore handles offline
                is ReviewState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    btnSubmit.isEnabled = true
                    btnSubmit.text      = "Submit Review"
                    viewModel.resetState()
                }
                is ReviewState.Idle -> {
                    btnSubmit.isEnabled = true
                    btnSubmit.text      = "Submit Review"
                }
            }
        }
    }
}