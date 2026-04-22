package com.example.focuskey.ui.minigames

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.focuskey.data.KeyManager
import com.example.focuskey.R
import kotlin.math.min

class DragPuzzleActivity : AppCompatActivity() {

    private lateinit var boardArea: FrameLayout
    private lateinit var trayArea: FrameLayout
    private lateinit var dragLayer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var soundPool: SoundPool

    private val gridSize = 3
    private val totalPieces = 9
    private val trayColumns = 3

    private data class Piece(
        val bitmap: Bitmap,
        val originalIndex: Int,
        var currentRotation: Int = 0
    )

    private val pieces = mutableListOf<Piece>()
    private val boardState = arrayOfNulls<Piece>(totalPieces)

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var rotationRunnable: Runnable? = null

    private var gameActive = false
    private var gameFinished = false
    private var gameStartTime = 0L
    private val totalDuration = 150_000L

    private var rotatingPiece: Piece? = null
    private var rotatingView: ImageView? = null

    private var draggingPiece: Piece? = null
    private var draggingOriginalView: ImageView? = null
    private var floatingView: ImageView? = null

    private var activeRotationAnimator: ObjectAnimator? = null

    private var pressSoundId = 0
    private var rotateSoundId = 0

    private var startButtonRef: Button? = null
    private lateinit var keysTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drag_puzzle)

        boardArea = findViewById(R.id.board_area)
        trayArea = findViewById(R.id.tray_area)
        dragLayer = findViewById(R.id.drag_layer)
        progressBar = findViewById(R.id.progress_bar)

        progressBar.max = totalDuration.toInt()
        progressBar.progress = totalDuration.toInt()

        setSupportActionBar(findViewById(R.id.toolbar))
        WindowCompat.setDecorFitsSystemWindows(window, true)
        keysTextView = findViewById(R.id.text_key_count)
        supportActionBar?.title = "Пазл"

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

        pressSoundId = soundPool.load(this, R.raw.short_sound, 1)
        rotateSoundId = soundPool.load(this, R.raw.long_sound, 1)

        showDialog()
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
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

    private fun startGame() {
        handler.removeCallbacksAndMessages(null)
        stopRotation()

        gameActive = true
        gameFinished = false
        gameStartTime = SystemClock.elapsedRealtime()
        progressBar.progress = totalDuration.toInt()

        val bitmap = getRandomImageFromAssets() ?: return
        splitBitmap(bitmap)

        for (i in 0 until totalPieces) {
            boardState[i] = null
        }

        pieces.shuffle()
        pieces.forEach {
            it.currentRotation = listOf(0, 90, 180, 270).random()
        }

        boardArea.post {
            renderBoard()
            renderTray()
            startProgress()
        }
    }

    private fun getRandomImageFromAssets(): Bitmap? {
        return try {
            val files = assets.list("images") ?: return null

            val imageFiles = files.filter {
                it.endsWith(".jpg", true)
            }

            val shuffled = imageFiles.shuffled()

            for (fileName in shuffled) {
                try {
                    val input = assets.open("images/$fileName")
                    android.util.Log.d("PUZZLE_IMAGE", "Selected image: $fileName")
                    val bitmap = BitmapFactory.decodeStream(input)
                    input.close()

                    if (bitmap != null) {
                        return bitmap
                    }
                } catch (_: Exception) {
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun splitBitmap(bitmap: Bitmap) {
        pieces.clear()

        val side = min(bitmap.width, bitmap.height)
        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, left, top, side, side)

        val pieceSize = side / gridSize

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = col * pieceSize
                val y = row * pieceSize
                val pieceBitmap = Bitmap.createBitmap(squareBitmap, x, y, pieceSize, pieceSize)
                pieces.add(
                    Piece(
                        bitmap = pieceBitmap,
                        originalIndex = row * gridSize + col
                    )
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderBoard() {
        boardArea.removeAllViews()

        if (boardArea.width == 0 || boardArea.height == 0) return

        val cellSize = min(boardArea.width, boardArea.height) / gridSize
        val boardWidth = cellSize * gridSize
        val boardHeight = cellSize * gridSize
        val startX = (boardArea.width - boardWidth) / 2f
        val startY = (boardArea.height - boardHeight) / 2f

        for (i in 0 until totalPieces) {
            val piece = boardState[i]
            val col = i % gridSize
            val row = i / gridSize

            val cell = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(cellSize, cellSize)
                x = startX + col * cellSize
                y = startY + row * cellSize
                scaleType = ImageView.ScaleType.FIT_XY
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)

                if (piece != null) {
                    setImageBitmap(piece.bitmap)
                    rotation = piece.currentRotation.toFloat()
                } else {
                    setImageDrawable(null)
                }

                setOnClickListener {
                    if (!gameActive) return@setOnClickListener
                    if (piece != null) {
                        returnPieceFromBoard(i)
                    }
                }

                setOnLongClickListener {
                    if (!gameActive) return@setOnLongClickListener false
                    if (piece != null) {
                        startBoardRotation(piece, this)
                        true
                    } else {
                        false
                    }
                }

                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        stopRotation()
                    }
                    false
                }
            }

            boardArea.addView(cell)
        }
    }

    private fun renderTray() {
        trayArea.removeAllViews()

        if (trayArea.width == 0 || trayArea.height == 0) return

        val rows = ((pieces.size + trayColumns - 1) / trayColumns).coerceAtLeast(1)
        val cellSize = min(trayArea.width / trayColumns, trayArea.height / rows)
        val trayWidth = cellSize * trayColumns
        val trayHeight = cellSize * rows
        val startX = (trayArea.width - trayWidth) / 2f
        val startY = (trayArea.height - trayHeight) / 2f

        pieces.forEachIndexed { index, piece ->
            val col = index % trayColumns
            val row = index / trayColumns

            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(cellSize, cellSize)
                x = startX + col * cellSize
                y = startY + row * cellSize
                scaleType = ImageView.ScaleType.FIT_XY
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)
                setImageBitmap(piece.bitmap)
                rotation = piece.currentRotation.toFloat()
                setOnTouchListener(createTrayTouchListener(piece, this))
            }

            trayArea.addView(imageView)
        }
    }

    private fun animateQuarterTurn(view: ImageView, piece: Piece) {
        activeRotationAnimator?.cancel()

        playSound(rotateSoundId)

        val start = piece.currentRotation.toFloat()
        var end = start + 90f
        if (end >= 360f) {
            end = 360f
        }

        activeRotationAnimator = ObjectAnimator.ofFloat(view, View.ROTATION, start, end).apply {
            duration = 220
            interpolator = DecelerateInterpolator()

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    piece.currentRotation = (piece.currentRotation + 90) % 360
                    view.rotation = piece.currentRotation.toFloat()
                    checkWinCondition()
                }
            })

            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createTrayTouchListener(piece: Piece, view: ImageView): View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!gameActive) return@OnTouchListener false
                    startTrayDrag(piece, view, event)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!gameActive || draggingPiece !== piece) return@OnTouchListener false
                    moveFloatingView(event)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!gameActive || draggingPiece !== piece) return@OnTouchListener false
                    finishTrayDrag(event)
                    true
                }

                else -> false
            }
        }
    }

    private fun startTrayDrag(piece: Piece, originalView: ImageView, event: MotionEvent) {
        stopRotation()
        playSound(pressSoundId)
        draggingPiece = piece
        draggingOriginalView = originalView

        val cellSize = min(boardArea.width, boardArea.height) / gridSize
        val params = FrameLayout.LayoutParams(cellSize, cellSize)

        floatingView = ImageView(this).apply {
            layoutParams = params
            scaleType = ImageView.ScaleType.FIT_XY
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            setImageBitmap(piece.bitmap)
            rotation = piece.currentRotation.toFloat()
        }

        val loc = IntArray(2)
        dragLayer.getLocationOnScreen(loc)

        floatingView?.let {
            it.x = event.rawX - loc[0] - cellSize / 2f
            it.y = event.rawY - loc[1] - cellSize / 2f
            dragLayer.addView(it)
            originalView.visibility = View.INVISIBLE
        }

        startRotation(piece, floatingView)
    }

    private fun moveFloatingView(event: MotionEvent) {
        val view = floatingView ?: return
        val loc = IntArray(2)
        dragLayer.getLocationOnScreen(loc)
        view.x = event.rawX - loc[0] - view.width / 2f
        view.y = event.rawY - loc[1] - view.height / 2f
    }

    private fun finishTrayDrag(event: MotionEvent) {
        stopRotation()

        val piece = draggingPiece
        val originalView = draggingOriginalView
        val view = floatingView

        val targetCell = findTargetCell(event.rawX, event.rawY)
        var placed = false

        if (piece != null && targetCell != null && boardState[targetCell] == null) {
            boardState[targetCell] = piece
            pieces.removeAll { it === piece }
            placed = true
        }

        if (view != null) {
            dragLayer.removeView(view)
        }

        if (placed) {
            renderBoard()
            renderTray()
            checkWinCondition()
        } else {
            originalView?.apply {
                setImageBitmap(piece?.bitmap)
                rotation = piece?.currentRotation?.toFloat() ?: 0f
                visibility = View.VISIBLE
            }
        }

        draggingPiece = null
        draggingOriginalView = null
        floatingView = null
    }

    private fun findTargetCell(rawX: Float, rawY: Float): Int? {
        val loc = IntArray(2)
        boardArea.getLocationOnScreen(loc)

        val left = loc[0]
        val top = loc[1]
        val cellSize = min(boardArea.width, boardArea.height) / gridSize
        val boardWidth = cellSize * gridSize
        val boardHeight = cellSize * gridSize
        val startX = left + (boardArea.width - boardWidth) / 2f
        val startY = top + (boardArea.height - boardHeight) / 2f

        val relativeX = rawX - startX
        val relativeY = rawY - startY

        if (relativeX < 0f || relativeY < 0f) return null
        if (relativeX >= boardWidth || relativeY >= boardHeight) return null

        val col = (relativeX / cellSize).toInt()
        val row = (relativeY / cellSize).toInt()

        if (col !in 0 until gridSize || row !in 0 until gridSize) return null
        return row * gridSize + col
    }

    private fun returnPieceFromBoard(index: Int) {
        playSound(pressSoundId)
        val piece = boardState[index] ?: return
        boardState[index] = null
        pieces.add(piece)
        renderBoard()
        renderTray()
    }

    private fun startBoardRotation(piece: Piece, view: ImageView) {
        stopRotation()
        rotatingPiece = piece
        rotatingView = view

        rotationRunnable = object : Runnable {
            override fun run() {
                val p = rotatingPiece ?: return
                val v = rotatingView ?: return
                if (!gameActive) return

                animateQuarterTurn(v, p)
                handler.postDelayed(this, 1000)
            }
        }

        handler.postDelayed(rotationRunnable!!, 1000)
    }

    private fun startRotation(piece: Piece, view: ImageView?) {
        stopRotation()
        if (view == null) return
        rotatingPiece = piece
        rotatingView = view

        rotationRunnable = object : Runnable {
            override fun run() {
                val p = rotatingPiece ?: return
                val v = rotatingView ?: return
                if (!gameActive) return

                animateQuarterTurn(v, p)
                handler.postDelayed(this, 1000)
            }
        }

        handler.postDelayed(rotationRunnable!!, 1000)
    }

    private fun stopRotation() {
        rotationRunnable?.let { handler.removeCallbacks(it) }
        rotationRunnable = null
        activeRotationAnimator?.cancel()
        activeRotationAnimator = null
        rotatingPiece = null
        rotatingView = null
    }

    private fun startProgress() {
        progressRunnable = object : Runnable {
            override fun run() {
                if (!gameActive) return

                val elapsed = SystemClock.elapsedRealtime() - gameStartTime
                val remaining = (totalDuration - elapsed).coerceAtLeast(0L)
                progressBar.progress = remaining.toInt()

                if (remaining > 0) {
                    handler.postDelayed(this, 100)
                } else {
                    endGame()
                }
            }
        }

        handler.post(progressRunnable!!)
    }

    private fun checkWinCondition() {
        if (!gameActive || gameFinished) return

        for (i in 0 until totalPieces) {
            val piece = boardState[i] ?: return
            if (piece.originalIndex != i) return
            if (piece.currentRotation % 360 != 0) return
        }

        gameFinished = true
        gameActive = false
        handler.removeCallbacksAndMessages(null)

        handler.postDelayed({
            showDialog()
        }, 1000)
    }

    private fun endGame() {
        if (!gameActive) return
        gameActive = false
        gameFinished = true
        handler.removeCallbacksAndMessages(null)
        showDialog()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        soundPool.release()
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (!gameActive) super.onBackPressed()
    }
}