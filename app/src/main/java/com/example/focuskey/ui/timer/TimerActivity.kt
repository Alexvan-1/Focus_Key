package com.example.focuskey.ui.timer

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.focuskey.databinding.FragmentTimerBinding

class TimerActivity : Fragment() {

    private var _binding: FragmentTimerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var mins = 20
    private lateinit var timer_text_choose: TextView

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

        val start_button: Button = binding.startButton

        val submit_button: Button = binding.submitButton
        val plus_button: Button = binding.buttonPlus
        val minus_button: Button = binding.buttonMinus

        val timer_text: TextView = binding.timerText
        timer_text_choose = binding.timerTextChoose

        var list = listOf(submit_button, plus_button, minus_button, timer_text_choose)

        start_button.setOnClickListener() {
            start_button.visibility = View.INVISIBLE
            start_button.isEnabled = false

            for (i in list) {
                i.visibility = View.VISIBLE
                i.isEnabled = true
            }

            updateTimeDisplay()
        }

        plus_button.setOnClickListener() {
            if (mins < 120) {
                mins += 20
                updateTimeDisplay()
            }
        }

        minus_button.setOnClickListener() {
            if (mins > 20) {
                mins -= 20
                updateTimeDisplay()
            }
        }

        submit_button.setOnClickListener() {
            timer_text.visibility = View.VISIBLE
            timer_text.isEnabled = true

            object : CountDownTimer((mins * 60000).toLong(), 1000) {

                override fun onTick(millisUntilFinished: Long) {
                    val hours = millisUntilFinished / 3600000
                    val minutes = (millisUntilFinished % 3600000) / 60000
                    val seconds = (millisUntilFinished % 60000) / 1000

                    timer_text.text = String.format("%01d:%02d:%02d", hours, minutes, seconds)
                }

                override fun onFinish() {
                    timer_text.text = "0:00:00"
                }
            }.start()

            for (i in list) {
                i.visibility = View.INVISIBLE
                i.isEnabled = false
            }
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
        _binding = null
    }
}