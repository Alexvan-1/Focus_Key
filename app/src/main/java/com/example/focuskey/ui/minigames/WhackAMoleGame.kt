package com.example.focuskey.ui.minigames

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.media.AudioAttributes
import android.media.SoundPool
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import androidx.lifecycle.Observer
import com.example.focuskey.data.KeyManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.focuskey.R
import com.example.focuskey.ui.setupKeysActionBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class WhackAMoleGame : AppCompatActivity() {

    private lateinit var holesContainer: ViewGroup
    private lateinit var scoreTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var soundPool: SoundPool

    private data class Hole(
        val container: FrameLayout,
        val moleView: ImageView
    )

    private val holes = mutableListOf<Hole>()
    private var activeHole: Hole? = null
    private var activeGifDrawable: GifDrawable? = null

    private var score = 0
    private var remainingAppearances = 20
    private var gameActive = false

    private val handler = Handler(Looper.getMainLooper())
    private var spawnRunnable: Runnable? = null
    private var progressRunnable: Runnable? = null
    private var hideMoleRunnable: Runnable? = null
    private var restoreHoleRunnable: Runnable? = null

    private var gameStartTime = 0L

    private val totalDuration = 63_000L
    private val spawnInterval = 3_000L

    private val holeSizeDp = 96
    private val holePaddingDp = 14

    private var moleStartSoundId = 0
    private var moleEndSoundId = 0
    private var hitSoundId = 0

    private var startButtonRef: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whack_a_mole)

        holesContainer = findViewById(R.id.holes_container)
        scoreTextView = findViewById(R.id.score_text)
        progressBar = findViewById(R.id.progress_bar)

        progressBar.max = totalDuration.toInt()
        progressBar.progress = totalDuration.toInt()

        supportActionBar?.title = "Ударь крота"
        setupKeysActionBar()

        KeyManager.keysLiveData.observe(this) { keyCount ->
            startButtonRef?.isEnabled = keyCount > 0
            startButtonRef?.alpha = if (keyCount > 0) 1f else 0.5f
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        moleStartSoundId = soundPool.load(this, R.raw.long_sound, 1)
        moleEndSoundId = soundPool.load(this, R.raw.short_sound, 1)
        hitSoundId = soundPool.load(this, R.raw.hit, 1)

        showDialog()
    }

    private fun showDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_start_exit, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnStart = view.findViewById<Button>(R.id.btn_start)
        val btnExit = view.findViewById<Button>(R.id.btn_exit)

        startButtonRef = btnStart

        val currentKeys = KeyManager.getKeys()
        btnStart.isEnabled = currentKeys > 0
        btnStart.alpha = if (currentKeys > 0) 1f else 0.5f

        btnStart.setOnClickListener {
            if (!KeyManager.spendKeys(1)) {
                btnStart.isEnabled = false
                btnStart.alpha = 0.5f
                return@setOnClickListener
            }

            dialog.dismiss()
            startGame()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.setOnDismissListener {
            if (startButtonRef === btnStart) {
                startButtonRef = null
            }
        }

        dialog.show()
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun startGame() {
        score = 0
        remainingAppearances = 20
        gameActive = true
        activeHole = null

        scoreTextView.text = "0/20"
        progressBar.max = totalDuration.toInt()
        progressBar.progress = totalDuration.toInt()

        handler.removeCallbacksAndMessages(null)

        createHoles()
    }

    private fun createHoles() {
        holesContainer.removeAllViews()
        holes.clear()

        val width = holesContainer.width
        val height = holesContainer.height

        if (width == 0 || height == 0) {
            holesContainer.post { createHoles() }
            return
        }

        val holeSize = dp(holeSizeDp)
        val padding = dp(holePaddingDp)

        val cols = 3
        val rows = 4

        val cellWidth = width / cols
        val cellHeight = height / rows

        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                cells.add(col to row)
            }
        }

        val selectedCells = cells.shuffled().take(10)

        selectedCells.forEach { (col, row) ->
            val left = col * cellWidth
            val top = row * cellHeight

            val maxXOffset = (cellWidth - holeSize - padding * 2).coerceAtLeast(0)
            val maxYOffset = (cellHeight - holeSize - padding * 2).coerceAtLeast(0)

            val x = left + padding + if (maxXOffset > 0) Random.nextInt(maxXOffset + 1) else 0
            val y = top + padding + if (maxYOffset > 0) Random.nextInt(maxYOffset + 1) else 0

            val container = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(holeSize, holeSize)
                this.x = x.toFloat()
                this.y = y.toFloat()
                isClickable = true
                isFocusable = true
            }

            val holeBg = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setImageResource(R.drawable.hole_background)
                isClickable = false
                isFocusable = false
            }

            val moleView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
                visibility = View.INVISIBLE
            }

            container.addView(holeBg)
            container.addView(moleView)
            moleView.bringToFront()
            container.setOnClickListener { onHoleClick(container) }

            holesContainer.addView(container)
            holes.add(Hole(container, moleView))
        }

        startProgress()
        startSpawning()
    }

    private fun startSpawning() {
        gameStartTime = SystemClock.elapsedRealtime()

        spawnRunnable = object : java.lang.Runnable {
            override fun run() {
                if (!gameActive) {
                    return
                }

                if (remainingAppearances <= 0) {
                    return
                }

                showMole()
                remainingAppearances--
                if (remainingAppearances > 0) {
                    handler.postDelayed(this, spawnInterval)
                }
            }
        }
        handler.postDelayed(spawnRunnable as java.lang.Runnable, spawnInterval)
    }

    private fun showMole() {
        if (holes.isEmpty()) return

        activeHole?.let {
            resetHole(it)
        }

        val hole = holes.random()
        activeHole = hole

        hideMoleRunnable?.let { handler.removeCallbacks(it) }

        hole.moleView.background = null
        hole.moleView.visibility = View.INVISIBLE
        hole.moleView.alpha = 0f

        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            hole.moleView.postInvalidate()
        }

        hole.moleView.setImageDrawable(null)

        Glide.with(this)
            .asGif()
            .load(R.drawable.mole_gif)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(null)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    resource.start()
                    activeGifDrawable = resource

                    playSound(moleStartSoundId)

                    val duration = 800L

                    hole.moleView.visibility = View.VISIBLE
                    hole.moleView.alpha = 1f

                    hideMoleRunnable = Runnable {
                        if (gameActive && activeHole == hole) {
                            playSound(moleEndSoundId)
                            activeGifDrawable?.stop()
                            activeGifDrawable = null
                            hole.moleView.setImageDrawable(null)
                            hole.moleView.visibility = View.INVISIBLE
                            hole.moleView.alpha = 0f
                            hole.moleView.postInvalidate()
                            activeHole = null
                        }
                    }
                    handler.postDelayed(hideMoleRunnable!!, duration)

                    return false
                }
            })
            .into(hole.moleView)
    }

    private fun onHoleClick(view: FrameLayout) {
        if (!gameActive) return

        val hole = holes.find { it.container == view } ?: return
        if (hole != activeHole) return

        score++
        scoreTextView.text = score.toString() + "/20"

        hideMoleRunnable?.let { handler.removeCallbacks(it) }
        activeGifDrawable?.stop()
        activeGifDrawable = null
        activeHole = null

        playSound(hitSoundId)
        playHitAnimation(hole)
    }

    private fun playHitAnimation(hole: Hole) {
        restoreHoleRunnable?.let { handler.removeCallbacks(it) }

        hole.moleView.background = null
        hole.moleView.visibility = View.INVISIBLE
        hole.moleView.alpha = 0f

        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            hole.moleView.postInvalidate()
        }

        hole.moleView.setImageDrawable(null)

        Glide.with(this)
            .asGif()
            .load(R.drawable.hit_animation)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(null)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    resource.start()

                    val duration = 1600L

                    hole.moleView.visibility = View.VISIBLE
                    hole.moleView.alpha = 1f

                    restoreHoleRunnable = Runnable {
                        if (gameActive) {
                            resource?.stop()
                            hole.moleView.setImageDrawable(null)
                            hole.moleView.visibility = View.INVISIBLE
                            hole.moleView.alpha = 0f
                            hole.moleView.postInvalidate()
                        }
                    }
                    handler.postDelayed(restoreHoleRunnable!!, duration)


                    return false

                }
            })
            .into(hole.moleView)


    }

    private fun startProgress() {
        progressRunnable = object : Runnable {
            override fun run() {
                if (!gameActive) return
                val elapsed = SystemClock.elapsedRealtime() - gameStartTime
                val remaining = (totalDuration - elapsed).coerceAtLeast(0L)
                progressBar.progress = remaining.toInt()
                if (remaining > 0) {
                    handler.post(progressRunnable!!)
                } else {
                    endGame()
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun endGame() {
        if (!gameActive) return
        gameActive = false
        handler.removeCallbacksAndMessages(null)

        activeGifDrawable?.stop()
        activeGifDrawable = null
        activeHole?.let {
            it.moleView.visibility = View.INVISIBLE
            it.moleView.alpha = 0f
        }
        activeHole = null

        showDialog()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun resetHole(hole: Hole) {
        activeGifDrawable?.stop()
        activeGifDrawable = null

        hole.moleView.setImageDrawable(null)
        hole.moleView.visibility = View.INVISIBLE
        hole.moleView.alpha = 0f
        hole.moleView.postInvalidate()

        activeHole = null
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (!gameActive) super.onBackPressed()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        holes.forEach {
            it.moleView.visibility = View.INVISIBLE
            it.moleView.alpha = 0f
        }
        activeGifDrawable?.stop()
        soundPool.release()
        super.onDestroy()
    }
}