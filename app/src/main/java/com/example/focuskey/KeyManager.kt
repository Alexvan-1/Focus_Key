package com.example.focuskey.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object KeyManager {

    private lateinit var prefs: SharedPreferences
    private val _keysLiveData = MutableLiveData<Int>()

    val keysLiveData: LiveData<Int> get() = _keysLiveData

    fun init(context: Context) {
        prefs = context.getSharedPreferences("focuskey_prefs", Context.MODE_PRIVATE)
        _keysLiveData.value = getKeys()
    }

    fun getKeys(): Int {
        if (!::prefs.isInitialized) return 0
        return prefs.getInt("keys_count", 0)
    }

    fun addKeys(amount: Int = 1) {
        val current = getKeys()
        prefs.edit().putInt("keys_count", current + amount).apply()
        _keysLiveData.postValue(current + amount)
    }

    fun spendKeys(amount: Int): Boolean {
        val current = getKeys()
        if (current >= amount) {
            prefs.edit().putInt("keys_count", current - amount).apply()
            _keysLiveData.postValue(current - amount)
            return true
        }
        return false
    }


    fun reset() {
        prefs.edit().clear().apply()
        _keysLiveData.postValue(0)
    }
}