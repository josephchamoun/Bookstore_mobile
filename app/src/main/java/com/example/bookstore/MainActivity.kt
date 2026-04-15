package com.example.bookstore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.example.bookstore.network.RetrofitClient
import com.example.bookstore.network.SessionManager
import com.example.bookstore.ui.auth.LoginActivity
import com.example.bookstore.ui.cart.CartFragment
import com.example.bookstore.ui.catalog.CatalogFragment
import com.example.bookstore.ui.orders.OrdersFragment
import com.example.bookstore.ui.profile.ProfileFragment
import com.example.bookstore.viewmodel.CartViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val cartViewModel: CartViewModel by viewModels()
    private var cartCount: Int = 0

    // Receives the 401 broadcast from RetrofitClient interceptor
    private val unauthorizedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            logoutAndRedirect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        sessionManager = SessionManager(this)

        // Register 401 broadcast receiver
        // Replace the registerReceiver call with this:
        ContextCompat.registerReceiver(
            this,
            unauthorizedReceiver,
            IntentFilter(RetrofitClient.ACTION_UNAUTHORIZED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_catalog
            loadFragment(CatalogFragment())
        }

        cartViewModel.cartCount.observe(this) { count ->
            cartCount = count
            invalidateOptionsMenu()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catalog -> loadFragment(CatalogFragment())
                R.id.nav_orders  -> loadFragment(OrdersFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        cartViewModel.loadCart()
    }

    // Inflate the toolbar menu with logout item
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val hasItems = cartCount > 0

        menu.findItem(R.id.action_cart)?.apply {
            isEnabled = hasItems
            title = if (hasItems) "Cart ($cartCount)" else "Cart"
            icon = icon?.mutate()?.let { drawable ->
                DrawableCompat.wrap(drawable).apply {
                    DrawableCompat.setTint(
                        this,
                        ContextCompat.getColor(
                            this@MainActivity,
                            if (hasItems) R.color.accent_blue else R.color.text_hint
                        )
                    )
                    alpha = if (hasItems) 255 else 110
                }
            }
        }

        menu.findItem(R.id.action_logout)?.icon =
            menu.findItem(R.id.action_logout)?.icon?.mutate()?.let { drawable ->
                DrawableCompat.wrap(drawable).apply {
                    DrawableCompat.setTint(
                        this,
                        ContextCompat.getColor(this@MainActivity, R.color.error)
                    )
                }
            }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                if (cartCount > 0) {
                    loadFragment(CartFragment())
                }
                true
            }
            R.id.action_logout -> {
                logoutAndRedirect()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unauthorizedReceiver)
    }

    private fun logoutAndRedirect() {
        sessionManager.logout()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
