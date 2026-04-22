package com.example.focuskey.ui.timer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focuskey.R
import com.example.focuskey.data.SessionStorage
import com.example.focuskey.databinding.FragmentTimerBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TimerActivity : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by viewModels()
    private var was_paused = false

    private lateinit var tagRadioGroup: RadioGroup
    private var selectedTag: String = "Другое"

    private lateinit var staticImage: ImageView
    private lateinit var videoView: VideoView
    private lateinit var mediaController: MediaController

    private lateinit var session_time: TextView
    private lateinit var iv_time: ImageView
    private lateinit var session_tag: TextView
    private lateinit var tag_dot: ImageView

    private var isFirstSessionStart = true


    private var mins = 20L
    private lateinit var timer_text_choose: TextView
    private lateinit var state_text: TextView
    private lateinit var timer_text: TextView
    private lateinit var count_down: TextView
    private lateinit var cancel_button: Button
    private lateinit var start_button: Button
    private lateinit var pause_button: Button
    private var count_down_timer: CountDownTimer? = null
    private var animator_button: android.view.ViewPropertyAnimator? = null
    private var animator_text: android.view.ViewPropertyAnimator? = null
    private val timeValues = listOf(20, 40, 60, 80, 100, 120)

    private var timerListener: TimerStateListener? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        staticImage = view.findViewById(R.id.imageView)
        videoView = view.findViewById(R.id.videoView)

        setupVideoPlayer()
    }

    private fun setupVideoPlayer() {
        mediaController = MediaController(requireContext())
        mediaController.setVisibility(View.GONE)

        val videoUri = Uri.parse("android.resource://" + requireContext().packageName + "/" + R.raw.focus_anim_fixed)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            if (viewModel.videoPosition > 0) {
                videoView.seekTo(viewModel.videoPosition)
            }
        }

        videoView.setOnErrorListener { mp, what, extra ->
            Log.e("VideoView", "Ошибка воспроизведения: $what, $extra")
            true
        }
    }

    private fun setSessionMode(isActive: Boolean) {
        if (isActive) {
            staticImage.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            videoView.seekTo(0)
            videoView.start()
        } else {
            videoView.pause()
            videoView.visibility = View.GONE
            staticImage.visibility = View.VISIBLE
        }
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        timerListener = context as? TimerStateListener
    }

    override fun onDetach() {
        super.onDetach()
        timerListener = null
    }


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        timer_text = binding.timerText
        cancel_button = binding.cancelButton
        start_button = binding.startButton
        count_down = binding.countDown
        state_text = binding.stateText
        pause_button = binding.pauseButton
        session_time = binding.sessionTime
        session_tag = binding.sessionTag
        tag_dot = binding.tagDot
        iv_time = binding.timeDot

        viewModel.switchToTimerEvent.observe(viewLifecycleOwner) {
            timerListener?.forceSwitchToTimer()
        }

        viewModel.saveSessionEvent.observe(viewLifecycleOwner) { session ->
            SessionStorage.getInstance(requireContext()).addSession(session)
        }

        start_button.setOnClickListener {
            start_button.visibility = View.INVISIBLE
            showTimePickerDialog()
        }

        cancel_button.setOnClickListener {
            cancelCurrentSession()
        }

        viewModel.displayTime.observe(viewLifecycleOwner) { time ->
            timer_text.text = time
        }

        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                TimerViewModel.TimerState.WORKING -> {
                    timerListener?.lockNavigation()
                    startTimerIconAnimation()
                    setNavIcons(true)
                    state_text.text = "Концентрация"
                    setSessionMode(true)
                    timer_text.visibility = View.VISIBLE
                    session_time.visibility = View.VISIBLE
                    session_tag.visibility = View.VISIBLE
                    tag_dot.visibility = View.VISIBLE
                    iv_time.visibility = View.VISIBLE

                    updateButtonsForWork(isFirstSessionStart)
                }

                TimerViewModel.TimerState.BREAK -> {
                    timerListener?.unlockNavigation()
                    stopTimerIconAnimation()
                    setNavIcons(false)
                    state_text.text = "Перерыв"
                    setSessionMode(false)
                    updateButtonsForBreak()
                }

                TimerViewModel.TimerState.IDLE -> {
                    timerListener?.unlockNavigation()
                    stopTimerIconAnimation()
                    setNavIcons(false)
                    state_text.text = " "
                    setSessionMode(false)
                    cancel_button.visibility = View.INVISIBLE
                    pause_button.visibility = View.INVISIBLE
                    start_button.visibility = View.VISIBLE
                    timer_text.visibility = View.INVISIBLE
                    session_time.visibility = View.INVISIBLE
                    session_tag.visibility = View.INVISIBLE
                    tag_dot.visibility = View.INVISIBLE
                    iv_time.visibility = View.INVISIBLE
                }

                else -> {}
            }
        }

        return root
    }

    private var rotationHandler: Handler? = null
    private var rotationRunnable: Runnable? = null
    private var currentRotationIndex = 0
    private val rotationFrames = listOf(
        R.drawable.timer_0,
        R.drawable.timer_45,
        R.drawable.timer_90,
        R.drawable.timer_135,
        R.drawable.timer_180,
        R.drawable.timer_225,
        R.drawable.timer_270,
        R.drawable.timer
    )

    private fun updateButtonsForWork(firstSession: Boolean) {
        if (firstSession) {
            cancel_button.visibility = View.VISIBLE
            cancel_button.alpha = 1f
            pause_button.visibility = View.INVISIBLE
        } else {
            cancel_button.visibility = View.INVISIBLE
            pause_button.visibility = View.VISIBLE
            pause_button.alpha = 1f
            pause_button.text = "Пауза"
        }
    }

    private fun updateButtonsForBreak() {
        cancel_button.visibility = View.VISIBLE
        cancel_button.alpha = 1f
        pause_button.visibility = View.INVISIBLE
    }

    private fun startTimerIconAnimation() {
        stopTimerIconAnimation()

        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.nav_view) ?: return
        val menuItem = bottomNav.menu.findItem(R.id.navigation_timer) ?: return

        currentRotationIndex = 0
        rotationHandler = Handler(Looper.getMainLooper())

        rotationRunnable = object : Runnable {
            override fun run() {
                menuItem.setIcon(rotationFrames[currentRotationIndex])
                bottomNav.invalidate()

                currentRotationIndex = (currentRotationIndex + 1) % rotationFrames.size
                rotationHandler?.postDelayed(this, 500)
            }
        }

        rotationHandler?.post(rotationRunnable!!)
    }

    private fun stopTimerIconAnimation() {
        rotationHandler?.removeCallbacksAndMessages(null)
        rotationHandler = null
        rotationRunnable = null

        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.nav_view) ?: return
        bottomNav.menu.findItem(R.id.navigation_timer).setIcon(R.drawable.timer)
    }

    private fun setNavIcons(isSessionActive: Boolean) {
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.nav_view)
        val historyItem = bottomNav?.menu?.findItem(R.id.navigation_history)
        val minigamesItem = bottomNav?.menu?.findItem(R.id.navigation_minigames)

        if (isSessionActive) {
            historyItem?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.locked)
            minigamesItem?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.locked)
        } else {
            historyItem?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.history)
            minigamesItem?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.gamepad)
        }
    }

    private fun ShowCountdownDialog() {
        val dialog_v = layoutInflater.inflate(R.layout.count_down_layout, null)
        val count_down3: TextView = dialog_v.findViewById(R.id.count_down3_text)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialog_v)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                count_down3.text = (seconds + 1).toString()
            }
            override fun onFinish() {
                dialog.dismiss()
                Log.d("TimerDebug", "Начало сессии с тегом: $selectedTag")
            }
        }.start()
        dialog.show()
    }

    private fun stop_anim() {
        animator_button?.cancel()
        animator_text?.cancel()
        animator_button = null
        animator_text = null
    }

    private fun setupTagSelection() {
        tagRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            selectedTag = when(checkedId) {
                R.id.tag_study -> "Учёба"
                R.id.tag_self_development -> "Саморазвитие"
                R.id.tag_sports -> "Спорт"
                R.id.tag_hobby -> "Хобби"
                R.id.tag_other -> "Другое"
                else -> "Другое"
            }
            Log.d("TimerDebug", "Выбран тег: $selectedTag")
        }
    }

    private fun showTimePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.time_picker, null)
        val timeSeekBar: SeekBar = dialogView.findViewById(R.id.seekBar)
        timer_text_choose = dialogView.findViewById(R.id.timer_text_choose)
        val submit_button: Button = dialogView.findViewById(R.id.submit_button)
        val back_button: Button = dialogView.findViewById(R.id.back_button)
        tagRadioGroup = dialogView.findViewById(R.id.tag_radio_group)
        setupTagSelection()
        tagRadioGroup.check(R.id.tag_other)

        timeSeekBar.max = timeValues.size - 1
        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mins = timeValues[progress].toLong()
                updateTimeDisplay()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        timeSeekBar.progress = 0
        updateTimeDisplay()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        val window = dialog.window
        if (window != null) {
            val params = window.attributes
            params.gravity = Gravity.BOTTOM
            params.x = 0
            params.y = 0
            window.attributes = params
        }

        submit_button.setOnClickListener {
            ShowCountdownDialog()
            dialog.dismiss()

            viewModel.selectedDurationMinutes = mins.toInt()
            viewModel.selectedTag = selectedTag

            cancel_button.postDelayed({
                if (isFirstSessionStart) {
                    cancel_button.visibility = View.VISIBLE
                    count_down.visibility = View.VISIBLE
                    pause_button.visibility = View.INVISIBLE

                    animator_button = cancel_button.animate()
                        .alpha(0f)
                        .setDuration(10000)
                        .withEndAction {
                            cancel_button.visibility = View.INVISIBLE
                            pause_button.visibility = View.VISIBLE
                            pause_button.alpha = 1f
                            pause_button.text = "Пауза"
                            isFirstSessionStart = false
                        }
                    animator_button?.start()

                    count_down_timer = object : CountDownTimer(10000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            val seconds = millisUntilFinished / 1000
                            count_down.text = seconds.toString()
                        }

                        override fun onFinish() {
                            count_down.visibility = View.INVISIBLE
                        }
                    }.start()

                    animator_text = count_down.animate()
                        .alpha(0f)
                        .setDuration(10000)

                    animator_text?.start()

                } else {
                    updateButtonsForWork(false)
                }

                viewModel.startWorkSession(mins)
                viewModel.startWorkTimer()

                if (mins < 60) {
                    session_time.text = "$mins мин"
                } else {
                    val h = mins / 60
                    val m = mins % 60
                    session_time.text = if (m == 0L) "$h ч" else "$h ч $m мин"
                }

                session_tag.text = selectedTag

                val color = when (selectedTag) {
                    "Учёба" -> R.color.color_study
                    "Саморазвитие" -> R.color.color_self_development
                    "Спорт" -> R.color.color_sports
                    "Хобби" -> R.color.color_hobby
                    else -> R.color.color_other
                }

                tag_dot.setColorFilter(ContextCompat.getColor(requireContext(), color))
            }, 3000)
        }

        back_button.setOnClickListener {
            dialog.dismiss()
            mins = 20
            updateTimeDisplay()
            start_button.visibility = View.VISIBLE
        }

        cancel_button.setOnClickListener {
            cancelCurrentSession()
        }

        pause_button.setOnClickListener {
            if (viewModel.isPausedLive.value == true) {
                viewModel.resumeTimer()
                startTimerIconAnimation()
                videoView.seekTo(viewModel.videoPosition)
                videoView.start()
                pause_button.text = "Пауза"
            } else {
                viewModel.pauseTimer()
                stopTimerIconAnimation()
                viewModel.videoPosition = videoView.currentPosition
                videoView.pause()
                pause_button.text = "Продолжить"
            }
        }

        viewModel.getKeysLiveData().observe(viewLifecycleOwner) { keysCount ->
            Log.e("KeysDebug", "🔑 $keysCount")
        }

        dialog.show()
    }

    private fun cancelCurrentSession() {
        stop_anim()
        viewModel.cancelAll()
        viewModel.videoPosition = 0
        was_paused = false
        count_down_timer?.cancel()
        timer_text.text = "0:00:00"
        timer_text.visibility = View.INVISIBLE
        start_button.visibility = View.VISIBLE
        cancel_button.alpha = 1f
        count_down.alpha = 1f
        cancel_button.visibility = View.INVISIBLE
        count_down.visibility = View.INVISIBLE
        pause_button.visibility = View.INVISIBLE
        isFirstSessionStart = true
        mins = 20
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeDisplay() {
        val hours = mins / 60
        val minutes = mins % 60
        val form_mins = String.format("%02d", minutes)
        timer_text_choose.text = "$hours:$form_mins:00"
    }

    override fun onResume() {
        super.onResume()

        when (viewModel.timerState.value) {
            TimerViewModel.TimerState.WORKING -> timerListener?.lockNavigation()
            TimerViewModel.TimerState.BREAK -> timerListener?.unlockNavigation()
            else -> timerListener?.unlockNavigation()
        }

        if (viewModel.isPausedLive.value == true && viewModel.timerState.value != TimerViewModel.TimerState.IDLE) {
            viewModel.resumeTimer()

            if (viewModel.timerState.value == TimerViewModel.TimerState.WORKING && was_paused) {
                videoView.seekTo(viewModel.videoPosition)
                videoView.start()
                was_paused = false
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (viewModel.timerState.value != TimerViewModel.TimerState.IDLE &&
            viewModel.isPausedLive.value != true
        ) {
            viewModel.pauseTimer()

            if (viewModel.timerState.value == TimerViewModel.TimerState.WORKING) {
                viewModel.videoPosition = videoView.currentPosition
                videoView.pause()
                was_paused = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stop_anim()
        _binding = null
    }
}