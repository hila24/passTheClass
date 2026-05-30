package com.example.obstaclesgame

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val ROWS = 6
    private val COLS = 3

    private lateinit var gameGrid: GridLayout
    private lateinit var txtLives: TextView
    private lateinit var txtScore: TextView
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button

    private val gridMatrix = Array(ROWS) { arrayOfNulls<ImageView>(COLS) }
    private val obstacleMatrix = Array(ROWS) { BooleanArray(COLS) { false } }

    private var playerColumn = 1
    private var lives = 3
    private var score = 0
    private var isGameOver = false
    private var lastStepHadObstacle = false

    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameGrid = findViewById(R.id.gameGrid)
        txtLives = findViewById(R.id.txtLives)
        txtScore = findViewById(R.id.txtScore)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)

        setupGrid()
        setupControls()
        startGameLoop()
    }

    private fun setupGrid() {
        gameGrid.rowCount = ROWS
        gameGrid.columnCount = COLS

        val displayMetrics = resources.displayMetrics
        val cellWidth = (displayMetrics.widthPixels - 100) / COLS
        val cellHeight = (displayMetrics.heightPixels * 3 / 4) / ROWS

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val imageView = ImageView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = cellWidth
                        height = cellHeight
                        rowSpec = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                    }
                    setBackgroundColor(0x0AFFFFFF)
                    setPadding(16, 16, 16, 16)
                }
                gridMatrix[r][c] = imageView
                gameGrid.addView(imageView)
            }
        }
        updateUI()
    }

    private fun setupControls() {
        btnLeft.setOnClickListener {
            if (playerColumn < COLS - 1 && !isGameOver) {
                playerColumn++
                updateUI()
            }
        }

        btnRight.setOnClickListener {
            if (playerColumn > 0 && !isGameOver) {
                playerColumn--
                updateUI()
            }
        }
    }

    private fun startGameLoop() {
        gameRunnable = Runnable {
            if (!isGameOver) {
                countEvadedObstacles()
                moveObstaclesDown()
                generateNewObstacle()
                checkCollision()
                updateUI()

                gameHandler.postDelayed(gameRunnable, 600)
            }
        }
        gameHandler.post(gameRunnable)
    }

    private fun countEvadedObstacles() {
        for (c in 0 until COLS) {
            if (obstacleMatrix[ROWS - 1][c] && c != playerColumn) {
                score += 5
                txtScore.text = "נקודות זכות: $score 🏆"
            }
        }
    }

    private fun moveObstaclesDown() {
        for (r in ROWS - 1 downTo 1) {
            for (c in 0 until COLS) {
                obstacleMatrix[r][c] = obstacleMatrix[r - 1][c]
            }
        }
        for (c in 0 until COLS) {
            obstacleMatrix[0][c] = false
        }
    }

    private fun generateNewObstacle() {
        val randomCol = Random.nextInt(COLS)
        if (lastStepHadObstacle) {
            if (Random.nextFloat() < 0.10f) {
                obstacleMatrix[0][randomCol] = true
                lastStepHadObstacle = true
            } else {
                lastStepHadObstacle = false
            }
        } else {
            if (Random.nextFloat() < 0.55f) {
                obstacleMatrix[0][randomCol] = true
                lastStepHadObstacle = true
            } else {
                lastStepHadObstacle = false
            }
        }
    }

    private fun checkCollision() {
        if (obstacleMatrix[ROWS - 1][playerColumn]) {
            lives--
            txtLives.text = "❤️ חיים: $lives"
            obstacleMatrix[ROWS - 1][playerColumn] = false

            vibrateOnCollision()

            if (lives <= 0) {
                isGameOver = true
                Toast.makeText(this, "נכשלת בקורס! נתראה במועד ב'", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "חטפת F! נשארו עוד $lives הזדמנויות", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun vibrateOnCollision() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    private fun updateUI() {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val imageView = gridMatrix[r][c] ?: continue

                val isPlayerPos = (r == ROWS - 1 && c == playerColumn)
                val hasObstacle = obstacleMatrix[r][c]

                when {
                    isPlayerPos -> {
                        imageView.setImageResource(R.drawable.student_player)
                        imageView.visibility = View.VISIBLE
                    }
                    hasObstacle -> {
                        imageView.setImageResource(R.drawable.fail_obstacle)
                        imageView.visibility = View.VISIBLE
                    }
                    else -> {
                        imageView.setImageDrawable(null)
                        imageView.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameHandler.removeCallbacks(gameRunnable)
    }
}