package com.example.focuskey.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionStorage(context: Context) {
    private val prefs = context.getSharedPreferences("focus_key", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSessions(sessions: List<Session>) {
        val json = gson.toJson(sessions)
        prefs.edit().putString("sessions", json).apply()
    }

    fun loadSessions(): List<Session> {
        val json = prefs.getString("sessions", "[]")
        val type = object : TypeToken<List<Session>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addSession(session: Session) {
        val list = loadSessions().toMutableList()
        list.add(0, session)
        saveSessions(list)
    }

    fun clearSessions() {
        prefs.edit().remove("sessions").apply()
    }
}