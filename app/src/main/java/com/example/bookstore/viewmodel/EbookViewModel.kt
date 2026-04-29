package com.example.bookstore.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bookstore.repository.EbookRepository
import com.example.bookstore.repository.PdfResult
import kotlinx.coroutines.launch
import java.io.File

class EbookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EbookRepository(application)

    private val _pdfState = MutableLiveData<PdfState>(PdfState.Idle)
    val pdfState: LiveData<PdfState> = _pdfState

    // ── Load PDF — checks cache first, downloads if needed ───────────────────
    fun loadPdf(bookId: String) {  // ← change Int to String
        if (_pdfState.value is PdfState.Ready) return
        viewModelScope.launch {
            _pdfState.value = PdfState.Loading
            when (val result = repository.loadPdf(bookId)) {
                is PdfResult.Ready -> _pdfState.value = PdfState.Ready(result.file)
                is PdfResult.Error -> _pdfState.value = PdfState.Error(result.message)
            }
        }
    }

    fun saveLastPage(bookId: String, page: Int) { // ← String
        getApplication<Application>()
            .getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
            .edit().putInt("last_page_$bookId", page).apply()
    }

    fun getLastPage(bookId: String): Int { // ← String
        return getApplication<Application>()
            .getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
            .getInt("last_page_$bookId", 0)
    }
}

// ── PdfState sealed class ─────────────────────────────────────────────────────
sealed class PdfState {
    object Idle                         : PdfState()
    object Loading                      : PdfState()
    data class Ready(val file: File)    : PdfState()
    data class Error(val message: String) : PdfState()
}