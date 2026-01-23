package com.example.focuskey

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import com.example.focuskey.data.KeyManager
import androidx.fragment.app.Fragment
import com.example.focuskey.databinding.ActivityMainBinding
import com.example.focuskey.ui.history.HistoryActivity
import com.example.focuskey.ui.minigames.MinigamesActivity
import com.example.focuskey.ui.timer.TimerActivity
import com.example.focuskey.ui.timer.TimerStateListener

class MainActivity : AppCompatActivity(), TimerStateListener {

    private lateinit var binding: ActivityMainBinding
    private val fragments = mutableMapOf<Int, Fragment>()
    private var currentTabId = R.id.navigation_timer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KeyManager.init(applicationContext)

        val currentKeys = KeyManager.getKeys()
        Log.e("KeysDebug","Текущие ключи при запуске: $currentKeys")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragments[R.id.navigation_history] = HistoryActivity()
        fragments[R.id.navigation_timer] = TimerActivity()
        fragments[R.id.navigation_minigames] = MinigamesActivity()

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, fragments[R.id.navigation_timer]!!, "timer")
            add(R.id.fragment_container, fragments[R.id.navigation_history]!!, "history").hide(fragments[R.id.navigation_history]!!)
            add(R.id.fragment_container, fragments[R.id.navigation_minigames]!!, "minigames").hide(fragments[R.id.navigation_minigames]!!)
        }.commit()

        val navView: BottomNavigationView = binding.navView

        navView.setOnItemSelectedListener { item ->
            switchFragment(item.itemId)
            true
        }

        navView.selectedItemId = R.id.navigation_timer
        supportActionBar?.title = "Таймер"
    }

    override fun lockNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNav.isEnabled = false
        bottomNav.menu.forEach { item ->
            item.isEnabled = false
        }
    }

    override fun unlockNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNav.isEnabled = true
        bottomNav.menu.forEach { item ->
            item.isEnabled = true
        }
    }
    private fun switchFragment(tabId: Int) {
        if (tabId == currentTabId) return

        val transaction = supportFragmentManager.beginTransaction()

        transaction.setCustomAnimations(
            android.R.animator.fade_in,
            android.R.animator.fade_out,
            android.R.animator.fade_in,
            android.R.animator.fade_out
        )

        fragments[currentTabId]?.let { transaction.hide(it) }

        fragments[tabId]?.let {
            transaction.show(it)
        }

        transaction.commit()
        currentTabId = tabId

        when(tabId) {
            R.id.navigation_history -> supportActionBar?.title = "История"
            R.id.navigation_timer -> supportActionBar?.title = "Таймер"
            R.id.navigation_minigames -> supportActionBar?.title = "Мини-игры"
        }

    }
}