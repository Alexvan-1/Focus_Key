package com.example.focuskey.ui.minigames

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.focuskey.R

class MemoryGameActivity : AppCompatActivity() {

    private lateinit var cardsGrid: GridLayout
    private lateinit var progressBar: ProgressBar

    private data class Card(
        val shapeRes: Int,
        val color: Int,
        var isMatched: Boolean = false,
        var isFlipped: Boolean = false,
        var container: FrameLayout? = null,
        var frontView: ImageView? = null
    )

    private val cardList = mutableListOf<Card>()
    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private var isProcessing = false

    private var gameActive = false
    private var gameStartTime = 0L
    private val totalDuration = 30_000L

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private val shapeResources = listOf(
        R.drawable.shape_circle,
        R.drawable.shape_square,
        R.drawable.shape_triangle,
        R.drawable.shape_trapezoid,
        R.drawable.shape_quadrilateral
    )

    private val colorList = listOf(
        Color.parseColor("#47C3B7"),
        Color.parseColor("#139018"),
        Color.parseColor("#FF7700"),
        Color.parseColor("#FF6200EE"),
        Color.parseColor("#FF3F51B5")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_game)

        cardsGrid = findViewById(R.id.cards_grid)
        progressBar = findViewById(R.id.progress_bar)

        progressBar.max = totalDuration.toInt()
        progressBar.progress = totalDuration.toInt()

        supportActionBar?.title = "Найди пару"

        showDialog()
    }

    private fun showDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_start_exit, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<Button>(R.id.btn_start).setOnClickListener {
            dialog.dismiss()
            startGame()
        }
        view.findViewById<Button>(R.id.btn_exit).setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }

    private fun startGame() {
        gameActive = true
        firstSelectedCard = null
        secondSelectedCard = null
        isProcessing = false
        progressBar.progress = totalDuration.toInt()
        gameStartTime = SystemClock.elapsedRealtime()

        initCards()
        shuffleCards()
        renderGrid()

        startProgress()
    }

    private fun initCards() {
        cardList.clear()
        val shapes = shapeResources.shuffled()
        val colors = colorList.shuffled()

        for (i in 0 until 5) {
            val shapeRes = shapes[i % shapes.size]
            val color = colors[i % colors.size]

            cardList.add(Card(shapeRes, color))
            cardList.add(Card(shapeRes, color))
        }
    }

    private fun shuffleCards() {
        cardList.shuffle()
    }

    private fun renderGrid() {
        cardsGrid.removeAllViews()
        cardsGrid.columnCount = 2
        cardsGrid.rowCount = 5

        for (i in cardList.indices) {
            val card = cardList[i]
            val cardView = createCardView(card)
            cardsGrid.addView(cardView)
        }
    }

    private fun createCardView(card: Card): View {
        val container = FrameLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener { onCardClick(card) }
        }

        val background = ImageView(this).apply {
            id = R.id.background
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(R.drawable.card_back)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(48, 48, 48, 48)
        }

        val front = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.INVISIBLE
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(48, 48, 48, 48)
        }

        container.addView(background)
        container.addView(front)

        card.container = container
        card.frontView = front
        return container
    }

    private fun onCardClick(card: Card) {
        if (!gameActive || isProcessing) return
        if (card.isMatched) return
        if (card.isFlipped) return

        if (firstSelectedCard != null && secondSelectedCard != null) return

        // открываем карту
        flipCard(card, true)

        if (firstSelectedCard == null) {
            firstSelectedCard = card
        } else if (secondSelectedCard == null && firstSelectedCard != card) {
            secondSelectedCard = card
            checkMatch()
        }
    }

    private fun flipCard(card: Card, show: Boolean) {
        if (show) {
            val front = card.frontView ?: return
            front.setImageResource(card.shapeRes)
            front.setColorFilter(card.color, android.graphics.PorterDuff.Mode.SRC_IN)
            front.visibility = View.VISIBLE
            card.container?.findViewById<ImageView>(R.id.background)?.setImageResource(R.drawable.card_front)

            card.isFlipped = true
        } else {
            card.frontView?.visibility = View.INVISIBLE
            card.frontView?.setImageDrawable(null)
            card.container?.findViewById<ImageView>(R.id.background)?.setImageResource(R.drawable.card_back)

            card.isFlipped = false
        }
    }

    private fun checkMatch() {
        val card1 = firstSelectedCard
        val card2 = secondSelectedCard
        if (card1 == null || card2 == null) return

        if (card1.shapeRes == card2.shapeRes && card1.color == card2.color) {
            // совпадение
            card1.isMatched = true
            card2.isMatched = true
            // оставляем открытыми, но отключаем клики
            card1.container?.isClickable = false
            card2.container?.isClickable = false
            firstSelectedCard = null
            secondSelectedCard = null

            // проверка победы
            if (cardList.all { it.isMatched }) {
                endGame()
            }
        } else {
            // несовпадение — закрываем через задержку
            isProcessing = true
            handler.postDelayed({
                flipCard(card1, false)
                flipCard(card2, false)
                firstSelectedCard = null
                secondSelectedCard = null
                isProcessing = false
            }, 1500)
        }
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

    private fun endGame() {
        if (!gameActive) return
        gameActive = false
        handler.removeCallbacks(progressRunnable!!)
        showDialog()
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (!gameActive) super.onBackPressed()
    }
}