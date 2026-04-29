package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.model.Review
import com.example.bookstore.repository.ReviewRepository
import com.example.bookstore.repository.SubmitResult
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReviewRepository()

    private val _reviewState = MutableLiveData<ReviewState>(ReviewState.Idle)
    val reviewState: LiveData<ReviewState> = _reviewState

    private val _averageRating = MutableLiveData<Float>(0f)
    val averageRating: LiveData<Float> = _averageRating

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEligible = MutableLiveData<Boolean>(false)
    val isEligible: LiveData<Boolean> = _isEligible

    private var currentBookId: String = ""

    // Reviews stream — auto updates via Firestore listener
    private val _reviews = MutableLiveData<List<Review>>()
    val reviews: LiveData<List<Review>> = _reviews

    fun init(bookId: String) {
        if (currentBookId == bookId) return
        currentBookId = bookId

        // Collect reviews flow
        viewModelScope.launch {
            repository.getReviews(bookId).collect { list ->
                _reviews.postValue(list)
                // Recompute average whenever reviews change
                if (list.isNotEmpty()) {
                    _averageRating.postValue(
                        list.map { it.rating }.average().toFloat()
                    )
                }
            }
        }

        checkEligibility(bookId)
    }

    fun submitReview(bookId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading
            when (val result = repository.submitReview(bookId, rating, comment)) {
                is SubmitResult.Success -> _reviewState.value = ReviewState.Success(result.message)
                is SubmitResult.Error   -> _reviewState.value = ReviewState.Error(result.message)
            }
        }
    }

    private fun checkEligibility(bookId: String) {
        viewModelScope.launch {
            _isEligible.postValue(repository.checkEligibility(bookId))
        }
    }

    fun resetState() { _reviewState.value = ReviewState.Idle }
}

sealed class ReviewState {
    object Idle                             : ReviewState()
    object Loading                          : ReviewState()
    data class Success(val message: String) : ReviewState()
    data class Error(val message: String)   : ReviewState()
}