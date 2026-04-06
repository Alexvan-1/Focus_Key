package com.example.focuskey.ui

import android.app.Activity
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.example.focuskey.R
import com.example.focuskey.data.KeyManager

fun AppCompatActivity.setupKeysActionBar() {
    supportActionBar?.apply {
        setDisplayShowCustomEnabled(true)
        setCustomView(R.layout.layout_actionbar_keys)
    }

    val customView = supportActionBar?.customView ?: return
    val keyText = customView.findViewById<TextView>(R.id.text_key_count)

    KeyManager.keysLiveData.observe(this as LifecycleOwner) { count ->
        keyText.text = count.toString()
    }
}