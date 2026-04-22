package com.example.bookstore.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bookstore.database.ReviewEntity
import com.example.bookstore.repository.ReviewRepository
import com.example.bookstore.repository.SubmitResult
import com.example.bookstore.worker.ReviewSyncWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReviewRepository(application)

    // ── Review state for submit operations ────────────────────────────────────
    private val _reviewState = MutableLiveData<ReviewState>(ReviewState.Idle)
    val reviewState: LiveData<ReviewState> = _reviewState

    // ── Average rating state ──────────────────────────────────────────────────
    private val _averageRating = MutableLiveData<Float>(0f)
    val averageRating: LiveData<Float> = _averageRating

    // ── Loading state ─────────────────────────────────────────────────────────
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // ── Current bookId — set once when fragment opens ─────────────────────────
    private var currentBookId: Int = -1

    // ── Reactive reviews stream — swapped when bookId changes ─────────────────
    private val _reviews = MutableLiveData<List<ReviewEntity>>()
    val reviews: LiveData<List<ReviewEntity>> = _reviews

    // ── Initialize with a bookId ──────────────────────────────────────────────
    fun init(bookId: Int) {
        if (currentBookId == bookId) return
        currentBookId = bookId


        _averageRating.value = repository.getCachedAverageRating(bookId)  // ADD THIS

        viewModelScope.launch {
            repository.observeReviews(bookId).collect { list: List<ReviewEntity> ->
                _reviews.postValue(list)
            }
        }

        refreshReviews(bookId)
        checkEligibility(bookId)
    }

    // ── Refresh from network — called from onResume ───────────────────────────
    fun refreshReviews(bookId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val avg = repository.refreshReviews(bookId)
                _averageRating.postValue(avg)
            } catch (e: Exception) {
                // Cached data still shows via Flow — no need to show error
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // ── Submit a new review ───────────────────────────────────────────────────
    fun submitReview(bookId: Int, rating: Int, comment: String) {
        if (rating < 1 || rating > 5) {
            _reviewState.value = ReviewState.Error("Please select a rating between 1 and 5")
            return
        }
        if (comment.isBlank()) {
            _reviewState.value = ReviewState.Error("Please write a comment")
            return
        }

        viewModelScope.launch {
            _reviewState.value = ReviewState.Loading
            when (val result = repository.submitReview(bookId, rating, comment)) {
                is SubmitResult.Success     -> {
                    _reviewState.value = ReviewState.Success(result.message)
                }
                is SubmitResult.SavedOffline -> {
                    _reviewState.value = ReviewState.SavedOffline
                    scheduleReviewSync()
                }
                is SubmitResult.Error       -> {
                    _reviewState.value = ReviewState.Error(result.message)
                }
            }
        }
    }

    // ── Schedule ReviewSyncWorker — identical to OrderSyncWorker pattern ───────
    private fun scheduleReviewSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ReviewSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(getApplication())
            .enqueue(request)
    }


    private val _isEligible = MutableLiveData<Boolean>(false)
    val isEligible: LiveData<Boolean> = _isEligible

    private fun checkEligibility(bookId: Int) {
        viewModelScope.launch {
            try {
                val result = repository.checkEligibility(bookId)
                _isEligible.postValue(result.eligible)
            } catch (e: Exception) {
                _isEligible.postValue(false)
            }
        }
    }

    // ── Reset state after toast is shown ──────────────────────────────────────
    fun resetState() {
        _reviewState.value = ReviewState.Idle
    }


}

// ── ReviewState sealed class ──────────────────────────────────────────────────
sealed class ReviewState {
    object Idle                         : ReviewState()
    object Loading                      : ReviewState()
    object SavedOffline                 : ReviewState()
    data class Success(val message: String) : ReviewState()
    data class Error(val message: String)   : ReviewState()
}