package com.example.focuskey.ui.timer

interface TimerStateListener {
    fun lockNavigation()
    fun unlockNavigation()
    fun forceSwitchToTimer()
}