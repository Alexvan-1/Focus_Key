package com.example.focuskey.ui.timer

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.focuskey.data.KeyManager
import com.example.focuskey.data.Session
import com.example.focuskey.utils.SingleLiveEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class TimerViewModel : ViewModel() {

    enum class TimerState {
        IDLE,
        COUNTDOWN,
        WORKING,
        BREAK
    }

    var selectedDurationMinutes: Int = 20
    var selectedTag: String = "Другое"

    private var isSessionSaved = false
    private var isPaused = false

    private var totalWorkMs: Long = 0L
    private var remainingWorkMs: Long = 0L
    private var currentPhaseRemainingMs: Long = 0L
    private var currentWorkBlockMs: Long = 0L

    var videoPosition: Int = 0

    private val _timerState = MutableLiveData(TimerState.IDLE)
    val timerState: LiveData<TimerState> = _timerState

    private val _displayTime = MutableLiveData<String>()
    val displayTime: LiveData<String> = _displayTime

    private val _saveSessionEvent = MutableLiveData<Session>()
    val saveSessionEvent: LiveData<Session> = _saveSessionEvent

    private val _isPaused = MutableLiveData(false)
    val isPausedLive: LiveData<Boolean> = _isPaused

    private val _switchToTimerEvent = SingleLiveEvent<Unit>()
    val switchToTimerEvent: LiveData<Unit> = _switchToTimerEvent

    private var activeTimer: CountDownTimer? = null
    private var breakTimer: CountDownTimer? = null
    private lateinit var currentDate: String
    private lateinit var currentTime: String

    private var sessionKeys = 0

    companion object {
        const val WORK_CYCLE = 20 * 60 * 1000L
        const val BREAK_DURATION = 5 * 60 * 1000L
    }

    fun startWorkSession(totalMinutes: Long) {
        cancelInternalTimers()
        currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        _timerState.value = TimerState.COUNTDOWN
        totalWorkMs = totalMinutes * 60_000L
        remainingWorkMs = totalWorkMs
        currentPhaseRemainingMs = 0L
        currentWorkBlockMs = 0L
        sessionKeys = 0
        isSessionSaved = false
        isPaused = false
        _isPaused.value = false
    }

    fun startWorkTimer() {
        if (_timerState.value == TimerState.WORKING || _timerState.value == TimerState.BREAK) return
        startNextWorkBlock()
    }

    private fun startNextWorkBlock() {
        if (remainingWorkMs <= 0L) {
            completeSession()
            return
        }

        _timerState.value = TimerState.WORKING
        currentWorkBlockMs = min(WORK_CYCLE, remainingWorkMs)

        startPhaseTimer(
            durationMs = currentWorkBlockMs,
            phase = TimerState.WORKING
        ) {
            if (currentWorkBlockMs == WORK_CYCLE) {
                sessionKeys++
                KeyManager.addKeys(1)
            }
            remainingWorkMs -= currentWorkBlockMs

            if (remainingWorkMs > 0L) {
                startBreakTimer()
            } else {
                completeSession()
            }
        }
    }

    private fun startBreakTimer() {
        _timerState.value = TimerState.BREAK
        startPhaseTimer(
            durationMs = BREAK_DURATION,
            phase = TimerState.BREAK
        ) {
            startNextWorkBlock()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (_timerState.value == TimerState.BREAK) {
                _switchToTimerEvent.call()
            }
        }, BREAK_DURATION - 3000)
    }

    private fun startPhaseTimer(
        durationMs: Long,
        phase: TimerState,
        onFinishAction: () -> Unit
    ) {
        cancelActiveTimerOnly()
        currentPhaseRemainingMs = durationMs
        isPaused = false
        _isPaused.value = false

        activeTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentPhaseRemainingMs = millisUntilFinished
                _displayTime.value = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                currentPhaseRemainingMs = 0L
                onFinishAction()
            }
        }.start()

        _timerState.value = phase
        _displayTime.value = formatTime(durationMs)
    }

    fun pauseTimer() {
        if (_timerState.value == TimerState.IDLE) return
        activeTimer?.cancel()
        breakTimer?.cancel()
        activeTimer = null
        breakTimer = null
        isPaused = true
        _isPaused.value = true
    }

    fun resumeTimer() {
        if (!isPaused) return
        if (currentPhaseRemainingMs <= 0L) return

        val phase = _timerState.value ?: TimerState.IDLE
        when (phase) {
            TimerState.WORKING -> {
                resumePhaseTimer(
                    durationMs = currentPhaseRemainingMs,
                    phase = TimerState.WORKING
                ) {
                    if (currentWorkBlockMs == WORK_CYCLE) {
                        sessionKeys++
                        KeyManager.addKeys(1)
                    }
                    remainingWorkMs -= currentWorkBlockMs
                    if (remainingWorkMs > 0L) {
                        startBreakTimer()
                    } else {
                        completeSession()
                    }
                }
            }

            TimerState.BREAK -> {
                resumePhaseTimer(
                    durationMs = currentPhaseRemainingMs,
                    phase = TimerState.BREAK
                ) {
                    startNextWorkBlock()
                }
            }

            else -> Unit
        }

        isPaused = false
        _isPaused.value = false
    }

    private fun resumePhaseTimer(
        durationMs: Long,
        phase: TimerState,
        onFinishAction: () -> Unit
    ) {
        cancelActiveTimerOnly()

        activeTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentPhaseRemainingMs = millisUntilFinished
                _displayTime.value = formatTime(millisUntilFinished)
            }

            override fun onFinish() {
                currentPhaseRemainingMs = 0L
                onFinishAction()
            }
        }.start()

        _timerState.value = phase
        _displayTime.value = formatTime(durationMs)
    }

    private fun completeSession() {
        if (isSessionSaved) return

        cancelInternalTimers()
        _timerState.value = TimerState.IDLE
        _displayTime.value = "0:00:00"
        remainingWorkMs = 0L
        currentPhaseRemainingMs = 0L
        currentWorkBlockMs = 0L

        saveSession(status = "Завершена")
        isSessionSaved = true
        sessionKeys = 0
    }

    fun cancelAll() {
        if (isSessionSaved && _timerState.value == TimerState.IDLE) return

        cancelInternalTimers()
        _timerState.value = TimerState.IDLE
        _displayTime.value = "0:00:00"

        if (selectedDurationMinutes > 0 && !isSessionSaved) {
            saveSession(status = "Отменена")
            isSessionSaved = true
        }

        remainingWorkMs = 0L
        currentPhaseRemainingMs = 0L
        currentWorkBlockMs = 0L
        totalWorkMs = 0L
        sessionKeys = 0
        isPaused = false
        _isPaused.value = false
    }

    private fun cancelInternalTimers() {
        activeTimer?.cancel()
        breakTimer?.cancel()
        activeTimer = null
        breakTimer = null
    }

    private fun cancelActiveTimerOnly() {
        activeTimer?.cancel()
        breakTimer?.cancel()
        activeTimer = null
        breakTimer = null
    }

    private fun saveSession(status: String) {
        val session = Session(
            startDate = currentDate,
            startTime = currentTime,
            durationMinutes = selectedDurationMinutes,
            tag = selectedTag,
            status = status,
            keysEarned = sessionKeys
        )
        _saveSessionEvent.value = session
    }

    private fun formatTime(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        return String.format("%01d:%02d:%02d", hours, minutes, seconds)
    }

    fun getKeysLiveData(): LiveData<Int> = KeyManager.keysLiveData

    override fun onCleared() {
        super.onCleared()
        cancelInternalTimers()
    }
}