package com.example.focuskey.ui.minigames

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.focuskey.R
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView


class InhaleExhale_game : AppCompatActivity() {

    private lateinit var textHandler: Handler
    private lateinit var textRunnable: Runnable
    private lateinit var progressHandler: Handler
    private lateinit var progressRunnable: Runnable
    private lateinit var text: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var vibrator: Vibrator


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inhale_exhale_game)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.title = "Вдох-выдох"

        text = findViewById(R.id.textView)
        progressBar = findViewById(R.id.progressBar)

        val dialogView = layoutInflater.inflate(R.layout.dialog_start_exit, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


        val btnStart = dialogView.findViewById<Button>(R.id.btn_start)
        val btnExit = dialogView.findViewById<Button>(R.id.btn_exit)

        btnStart.setOnClickListener {
            dialog.dismiss()
            val imageView = findViewById<ImageView>(R.id.imageView)
            Glide.with(this)
                .asGif()
                .load(R.drawable.inhale_exhale_anim)
                .listener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.startFromFirstFrame()
                        resource?.setLoopCount(7)

                        stopAnimations()

                        val duration = 7480L
                        val totalDuration = duration * 7L

                        val textChangeInterval = duration / 2
                        var isInhale = true
                        text.text = "Вдох"

                        textHandler = Handler(Looper.getMainLooper())
                        textRunnable = object : Runnable {
                            override fun run() {
                                isInhale = !isInhale
                                text.text = if (isInhale) "Вдох" else "Выдох"
                                vibrateTick()
                                textHandler.postDelayed(this, textChangeInterval)
                            }
                        }
                        textHandler.postDelayed(textRunnable, textChangeInterval)

                        val startTime = System.currentTimeMillis()
                        progressHandler = Handler(Looper.getMainLooper())
                        progressRunnable = object : Runnable {
                            override fun run() {
                                val elapsed = System.currentTimeMillis() - startTime
                                val remaining = (totalDuration - elapsed).coerceAtLeast(0)
                                val progress = (remaining * 100 / totalDuration).toInt()
                                progressBar.progress = progress
                                if (remaining > 0) {
                                    progressHandler.postDelayed(this, 50)
                                }
                            }
                        }
                        progressHandler.post(progressRunnable)

                        Handler(mainLooper).postDelayed({
                            stopAnimations()
                            dialog.show()
                        }, totalDuration)

                        return false
                    }
                })
                .into(imageView)
        }

        btnExit.setOnClickListener {
            finish()
            dialog.dismiss()
        }

        dialog.show()

    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {

    }

    private fun vibrateTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    private fun stopAnimations() {
        if (::textHandler.isInitialized) {
            textHandler.removeCallbacksAndMessages(null)
        }
        if (::progressHandler.isInitialized) {
            progressHandler.removeCallbacksAndMessages(null)
        }

        if (::progressBar.isInitialized) {
            progressBar.progress = 0
        }
        if (::text.isInitialized) {
            text.text = ""
        }
    }

    override fun onDestroy() {
        stopAnimations()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        stopAnimations()
    }
}