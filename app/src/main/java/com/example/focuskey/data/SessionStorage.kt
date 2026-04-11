package com.example.focuskey.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionStorage private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("focus_key", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _sessions = MutableLiveData<List<Session>>()
    val sessions: LiveData<List<Session>> = _sessions

    init {
        loadSessions()
    }

    private fun loadSessions() {
        val json = prefs.getString("sessions", "[]")
        val type = object : TypeToken<List<Session>>() {}.type
        _sessions.value = gson.fromJson(json, type)
    }

    fun addSession(session: Session) {
        val currentList = _sessions.value?.toMutableList() ?: mutableListOf()
        currentList.add(0, session)
        saveSessions(currentList)
        _sessions.value = currentList
    }

    private fun saveSessions(sessions: List<Session>) {
        val json = gson.toJson(sessions)
        prefs.edit().putString("sessions", json).apply()
    }

    fun clearSessions() {
        prefs.edit().remove("sessions").apply()
        _sessions.value = emptyList()
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionStorage? = null

        fun getInstance(context: Context): SessionStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}