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
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.focuskey.R
import com.example.focuskey.databinding.FragmentTimerBinding
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TimerActivity : Fragment() {

    private var _binding: FragmentTimerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var mins = 20

    private lateinit var timer_text_choose: TextView
    private lateinit var timer_text: TextView
    private lateinit var count_down: TextView

    private lateinit var cancel_button: Button
    private lateinit var start_button: Button

    private var currentTimer: CountDownTimer? = null
    private var count_down_timer: CountDownTimer? = null

    private var animator_button: android.view.ViewPropertyAnimator? = null
    private var animator_text: android.view.ViewPropertyAnimator? = null

    private val timeValues = listOf(20, 40, 60, 80, 100, 120)

    private var selectedTag: String = "Другое"

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

    private fun showTimePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.time_picker, null)

        val timeSeekBar: SeekBar = dialogView.findViewById(R.id.seekBar)

        timer_text_choose = dialogView.findViewById(R.id.timer_text_choose)

        val submit_button: Button = dialogView.findViewById(R.id.submit_button)
        val back_button: Button = dialogView.findViewById(R.id.back_button)

        timeSeekBar.max = timeValues.size - 1
        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mins = timeValues[progress]
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

        submit_button.setOnClickListener() {
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

                Log.d("TimerDebug", "Запуск таймера на $mins мин с тегом: $selectedTag")


                currentTimer = object : CountDownTimer((mins * 60000).toLong(), 1000) {

                    override fun onTick(millisUntilFinished: Long) {
                        val hours = millisUntilFinished / 3600000
                        val minutes = (millisUntilFinished % 3600000) / 60000
                        val seconds = (millisUntilFinished % 60000) / 1000

                        timer_text.text = String.format("%01d:%02d:%02d", hours, minutes, seconds)
                    }

                    override fun onFinish() {
                        timer_text.text = "0:00:00"

                        start_button.visibility = View.VISIBLE

                        cancel_button.alpha = 1f
                        count_down.alpha = 1f

                        stop_anim()
                    }


                }.start()
            }, 3000)

        }

        back_button.setOnClickListener() {
            dialog.dismiss()
            start_button.visibility = View.VISIBLE
        }

        cancel_button.setOnClickListener() {
            stop_anim()

            currentTimer?.cancel()
            currentTimer = null

            count_down_timer?.cancel()
            count_down_timer = null

            timer_text.text = "0:00:00"

            timer_text.visibility = View.INVISIBLE
            start_button.visibility = View.VISIBLE

            cancel_button.alpha = 1f
            count_down.alpha = 1f

            cancel_button.visibility = View.INVISIBLE
            count_down.visibility = View.INVISIBLE
            
            mins = 20
        }

        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val TimerLayout =
            ViewModelProvider(this).get(TimerLayout::class.java)

        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        timer_text = binding.timerText
        cancel_button = binding.cancelButton
        start_button = binding.startButton
        count_down = binding.countDown

        start_button.setOnClickListener() {
            start_button.visibility = View.INVISIBLE
            showTimePickerDialog()
        }

        return root
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeDisplay() {
        val hours = mins / 60
        val minutes = mins % 60

        val form_mins = String.format("%02d", minutes)
        timer_text_choose.text = "$hours:$form_mins:00"

    }

    override fun onDestroyView() {
        super.onDestroyView()
        stop_anim()
        _binding = null
    }
}