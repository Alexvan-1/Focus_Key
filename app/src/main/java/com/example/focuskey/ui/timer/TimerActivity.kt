package com.example.focuskey.ui.timer

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focuskey.R
import com.example.focuskey.databinding.FragmentTimerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.w3c.dom.Text

class TimerActivity : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TimerViewModel by viewModels()

    private lateinit var tagRadioGroup: RadioGroup
    private var selectedTag: String = "Другое"

    private var mins = 20L
    private lateinit var timer_text_choose: TextView
    private lateinit var state_text: TextView
    private lateinit var timer_text: TextView
    private lateinit var count_down: TextView
    private lateinit var cancel_button: Button
    private lateinit var start_button: Button
    private lateinit var pause_button: Button
    private var currentTimer: CountDownTimer? = null
    private var count_down_timer: CountDownTimer? = null
    private var animator_button: android.view.ViewPropertyAnimator? = null
    private var animator_text: android.view.ViewPropertyAnimator? = null
    private val timeValues = listOf(20, 40, 60, 80, 100, 120)

    private var timerListener: TimerStateListener? = null

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
                    if (viewModel.remainingWorkTime.value!! > 20) {
                        state_text.setText("До перерыва осталось:")
                    }
                    else {
                        state_text.setText("До конца осталось:")
                    }
                }
                TimerViewModel.TimerState.BREAK -> {
                    timerListener?.unlockNavigation()
                    state_text.setText("Перерыв")
                    cancel_button.visibility = View.VISIBLE
                    pause_button.visibility = View.INVISIBLE
                }
                TimerViewModel.TimerState.IDLE -> {
                    timerListener?.unlockNavigation()
                    state_text.setText(" ")
                }
                else -> {}
            }
        }

        return root
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
            selectedTag = when (checkedId) {
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
            params.gravity = Gravity.BOTTOM or Gravity.END
            params.x = 30
            params.y = 100
            window.attributes = params
        }

        submit_button.setOnClickListener {
            ShowCountdownDialog()
            dialog.dismiss()

            cancel_button.postDelayed({
                cancel_button.visibility = View.VISIBLE
                count_down.visibility = View.VISIBLE
                animator_button = cancel_button.animate()
                    .alpha(0f)
                    .setDuration(10000)
                    .withEndAction {
                        cancel_button.visibility = View.INVISIBLE
                        pause_button.visibility = View.VISIBLE
                        pause_button.animate()
                            .alpha(1f)
                            .setDuration(5000)
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

                timer_text.visibility = View.VISIBLE

                viewModel.startWorkSession(mins)
                viewModel.startWorkTimer()

            }, 3000)
        }

        back_button.setOnClickListener {
            dialog.dismiss()
            start_button.visibility = View.VISIBLE
        }

        cancel_button.setOnClickListener {
            cancelCurrentSession()
        }

        pause_button.setOnClickListener() {
            if (viewModel.isPausedLive.value == true) {
                viewModel.resumeTimer()
                pause_button.setText("Пауза")
            } else {
                viewModel.pauseTimer()
                pause_button.setText("Продолжить")
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

        count_down_timer?.cancel()

        timer_text.text = "0:00:00"
        timer_text.visibility = View.INVISIBLE
        start_button.visibility = View.VISIBLE
        cancel_button.alpha = 1f
        count_down.alpha = 1f
        cancel_button.visibility = View.INVISIBLE
        count_down.visibility = View.INVISIBLE

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
            TimerViewModel.TimerState.WORKING -> {
                timerListener?.lockNavigation()
                cancel_button.visibility = View.INVISIBLE
                pause_button.visibility = View.INVISIBLE
            }
            else -> timerListener?.unlockNavigation()
        }
        viewModel.getKeysLiveData().observe(viewLifecycleOwner) { keysCount ->
            Log.e("KeysDebug", "🔑 $keysCount")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stop_anim()
        _binding = null
    }
}