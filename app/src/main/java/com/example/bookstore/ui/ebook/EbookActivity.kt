package com.example.bookstore.ui.ebook

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookstore.R
import com.example.bookstore.viewmodel.EbookViewModel
import com.example.bookstore.viewmodel.PdfState
import java.io.File

class EbookActivity : AppCompatActivity() {

    private val viewModel: EbookViewModel by viewModels()
    private var bookId: String = ""  // ← CHANGED: Int → String

    companion object {
        private const val EXTRA_BOOK_ID    = "book_id"
        private const val EXTRA_BOOK_TITLE = "book_title"

        // ← CHANGED: bookId: Int → String
        fun start(context: Context, bookId: String, bookTitle: String) {
            context.startActivity(
                Intent(context, EbookActivity::class.java).apply {
                    putExtra(EXTRA_BOOK_ID, bookId)
                    putExtra(EXTRA_BOOK_TITLE, bookTitle)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ebook)

        // ← CHANGED: getStringExtra instead of getIntExtra
        bookId = intent.getStringExtra(EXTRA_BOOK_ID) ?: ""
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Ebook"

        if (bookId.isEmpty()) { finish(); return }

        val toolbar         = findViewById<Toolbar>(R.id.toolbar)
        val recyclerView    = findViewById<RecyclerView>(R.id.rvPages)
        val layoutLoading   = findViewById<LinearLayout>(R.id.layoutLoading)
        val layoutError     = findViewById<LinearLayout>(R.id.layoutError)
        val layoutPageBar   = findViewById<LinearLayout>(R.id.layoutPageBar)
        val tvPageIndicator = findViewById<TextView>(R.id.tvPageIndicator)
        val tvError         = findViewById<TextView>(R.id.tvError)
        val btnRetry        = findViewById<Button>(R.id.btnRetry)
        val tvLoadingMsg    = findViewById<TextView>(R.id.tvLoadingMessage)

        setSupportActionBar(toolbar)
        supportActionBar?.title = bookTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        btnRetry.setOnClickListener {
            tvLoadingMsg.text        = "Loading ebook..."
            layoutLoading.visibility = View.VISIBLE
            layoutError.visibility   = View.GONE
            viewModel.loadPdf(bookId)
        }

        viewModel.pdfState.observe(this) { state ->
            when (state) {
                is PdfState.Idle    -> { }
                is PdfState.Loading -> {
                    tvLoadingMsg.text        = "Downloading ebook..."
                    layoutLoading.visibility = View.VISIBLE
                    layoutError.visibility   = View.GONE
                    recyclerView.visibility  = View.GONE
                    layoutPageBar.visibility = View.GONE
                }
                is PdfState.Ready -> showPdf(
                    state.file, recyclerView, layoutLoading,
                    layoutError, layoutPageBar, tvPageIndicator
                )
                is PdfState.Error -> {
                    layoutLoading.visibility = View.GONE
                    layoutError.visibility   = View.VISIBLE
                    recyclerView.visibility  = View.GONE
                    layoutPageBar.visibility = View.GONE
                    tvError.text             = state.message
                }
            }
        }

        viewModel.loadPdf(bookId)
    }

    private fun showPdf(
        file: File,
        recyclerView: RecyclerView,
        layoutLoading: LinearLayout,
        layoutError: LinearLayout,
        layoutPageBar: LinearLayout,
        tvPageIndicator: TextView
    ) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(
                file, ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount   = pdfRenderer.pageCount
            val lastPage    = viewModel.getLastPage(bookId)

            val adapter = PdfPageAdapter(pdfRenderer, pageCount)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter       = adapter

            if (lastPage > 0) recyclerView.scrollToPosition(lastPage)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val currentPage = layoutManager.findFirstVisibleItemPosition()
                    if (currentPage >= 0) {
                        tvPageIndicator.text = "Page ${currentPage + 1} of $pageCount"
                        viewModel.saveLastPage(bookId, currentPage)
                    }
                }
            })

            tvPageIndicator.text     = "Page ${lastPage + 1} of $pageCount"
            layoutLoading.visibility = View.GONE
            layoutError.visibility   = View.GONE
            recyclerView.visibility  = View.VISIBLE
            layoutPageBar.visibility = View.VISIBLE

        } catch (e: Exception) {
            layoutLoading.visibility = View.GONE
            layoutError.visibility   = View.VISIBLE
            recyclerView.visibility  = View.GONE
            layoutPageBar.visibility = View.GONE
            findViewById<TextView>(R.id.tvError).text = "Failed to open PDF: ${e.message}"
        }
    }
}

class PdfPageAdapter(
    private val renderer: PdfRenderer,
    private val pageCount: Int
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page   = renderer.openPage(position)
        val width  = holder.imageView.width.takeIf { it > 0 } ?: 1080
        val height = (width * page.height / page.width.toFloat()).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        holder.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount() = pageCount
}