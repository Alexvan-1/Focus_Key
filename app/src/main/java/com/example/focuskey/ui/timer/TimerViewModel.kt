package com.example.focuskey.ui.timer

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.focuskey.data.Session
import com.example.focuskey.utils.SingleLiveEvent
import com.example.focuskey.data.KeyManager
import kotlin.math.min

class TimerViewModel: ViewModel() {

    enum class TimerState {
        IDLE,
        COUNTDOWN,
        WORKING,
        BREAK
    }

    var selectedDurationMinutes: Int = 20
    var selectedTag: String = "Другое"

    private var isPaused = false
    private var pausedTimeLeft: Long = 0L
    private var _isPaused = MutableLiveData(false)
    private var remainingMs: Long = 0L
    val isPausedLive: LiveData<Boolean> = _isPaused

    private var sessionKeys = 0

    var videoPosition: Int = 0

    private val _timerState = MutableLiveData(TimerState.IDLE)
    val timerState: LiveData<TimerState> = _timerState

    private val _displayTime = MutableLiveData<String>()
    val displayTime: LiveData<String> = _displayTime

    private val _remainingWorkTime = MutableLiveData<Long>(0)
    val remainingWorkTime: LiveData<Long> = _remainingWorkTime

    private val _saveSessionEvent = SingleLiveEvent<Session>()
    val saveSessionEvent: LiveData<Session> = _saveSessionEvent

    var currentTimer: CountDownTimer? = null
    private var breakTimer: CountDownTimer? = null

    companion object {
        const val WORK_CYCLE = 20 * 60 * 1000L
        const val BREAK_DURATION = 5 * 60 * 1000L
    }

    fun startWorkSession(totalMinutes: Long) {
        _timerState.value = TimerState.COUNTDOWN
        _remainingWorkTime.value = totalMinutes
    }

    fun startWorkTimer() {
        _timerState.value = TimerState.WORKING

        val workMinutes = min(20, _remainingWorkTime.value ?: 0).toInt()
        val duration = workMinutes * 60 * 1000L
        remainingMs = duration

        createTimer(remainingMs, workMinutes)

    }

    private fun createTimer(durationMs: Long, workMinutes: Int) {
        currentTimer?.cancel()
        currentTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                _displayTime.value = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                _displayTime.value = "0:00:00"

                if (workMinutes >= 20) {
                    sessionKeys++
                    KeyManager.addKeys(1)
                }

                val remaining = (_remainingWorkTime.value ?: 0) - workMinutes
                _remainingWorkTime.value = remaining

                if (remaining > 0 && workMinutes >= 20) {
                    startBreakTimer()
                } else {
                    completeSession()
                }
            }
        }.start()
    }

    private fun startBreakTimer() {
        _timerState.value = TimerState.BREAK
        _displayTime.value = formatTime(BREAK_DURATION)

        breakTimer?.cancel()
        breakTimer = object : CountDownTimer(BREAK_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _displayTime.value = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                startWorkTimer()
            }
        }.start()
    }

    fun pauseTimer() {
        currentTimer?.cancel()
        isPaused = true
        _isPaused.value = true
    }

    fun resumeTimer() {
        isPaused = false
        _isPaused.value = false

        createTimer(remainingMs, (remainingMs / 60000).toInt())

    }

    private fun completeSession() {
        _timerState.value = TimerState.IDLE
        _remainingWorkTime.value = 0
        saveSession(status = "Завершена")
        sessionKeys = 0
    }

    fun cancelAll() {
        isPaused = false
        _isPaused.value = false
        pausedTimeLeft = 0L
        currentTimer?.cancel()
        breakTimer?.cancel()
        _timerState.value = TimerState.IDLE
        _remainingWorkTime.value = 0
        if (selectedDurationMinutes > 0) {
            saveSession(status = "Отменена")
        }
    }

    private fun saveSession(status: String) {
        val currentDate = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val session = Session(
            startDate = currentDate,
            startTime = currentTime,
            durationMinutes = selectedDurationMinutes,
            tag = selectedTag,
            status = status
        )
        _saveSessionEvent.value = session
        sessionKeys = 0
    }

    private fun formatTime(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        return String.format("%01d:%02d:%02d", hours, minutes, seconds)
    }

    fun getKeysLiveData(): LiveData<Int> {
        return KeyManager.keysLiveData
    }

    override fun onCleared() {
        super.onCleared()
        cancelAll()
    }
}