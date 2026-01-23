package com.example.focuskey

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.focuskey.data.KeyManager
import com.example.focuskey.ui.timer.TimerStateListener
import com.example.focuskey.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), TimerStateListener  {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KeyManager.init(applicationContext)

        val currentKeys = KeyManager.getKeys()
        Log.e("KeysDebug","Текущие ключи при запуске: $currentKeys")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

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
}