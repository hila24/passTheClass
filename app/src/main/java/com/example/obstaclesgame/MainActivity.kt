package com.example.obstaclesgame

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        const val EXTRA_MODE = "extra_mode"
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    private val ROWS = 10
    private val COLS = 5

    private lateinit var gameGrid: GridLayout
    private lateinit var txtLives: TextView
    private lateinit var txtScore: TextView
    private lateinit var txtDistance: TextView
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var controlsLayout: View

    private val gridMatrix = Array(ROWS) { arrayOfNulls<ImageView>(COLS) }
    private val obstacleMatrix = Array(ROWS) { BooleanArray(COLS) { false } }
    private val coinMatrix = Array(ROWS) { BooleanArray(COLS) { false } }

    private var playerColumn = COLS / 2
    private var lives = 3
    private var score = 0
    private var distance = 0
    private var isGameOver = false
    private var lastStepHadObstacle = false

    private lateinit var mode: GameMode

    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable
    private var currentDelay = 600L

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastTiltMoveTime = 0L
    private val TILT_THRESHOLD = 3.0f
    private val TILT_COOLDOWN = 250L

    private var soundPool: SoundPool? = null
    private var crashSoundId = 0
    private var coinSoundId = 0

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val coinDrawableRes by lazy {
        resources.getIdentifier("coin", "drawable", packageName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mode = try {
            GameMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: GameMode.BUTTONS_SLOW.name)
        } catch (e: Exception) {
            GameMode.BUTTONS_SLOW
        }

        currentDelay = when (mode) {
            GameMode.BUTTONS_SLOW -> 700L
            GameMode.BUTTONS_FAST -> 350L
            GameMode.SENSOR -> 500L
        }

        gameGrid = findViewById(R.id.gameGrid)
        txtLives = findViewById(R.id.txtLives)
        txtScore = findViewById(R.id.txtScore)
        txtDistance = findViewById(R.id.txtDistance)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        controlsLayout = findViewById(R.id.controlsLayout)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationIfNeeded()

        setupSound()
        setupSensors()
        setupGrid()
        setupControls()
        startGameLoop()
    }

    private fun setupGrid() {
        gameGrid.rowCount = ROWS
        gameGrid.columnCount = COLS

        val displayMetrics = resources.displayMetrics
        val cellWidth = (displayMetrics.widthPixels - 80) / COLS
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
                    setPadding(8, 8, 8, 8)
                }
                gridMatrix[r][c] = imageView
                gameGrid.addView(imageView)
            }
        }
        updateUI()
    }

    private fun setupControls() {

        controlsLayout.visibility = if (mode == GameMode.SENSOR) View.GONE else View.VISIBLE

        btnRight.setOnClickListener { moveLeft() }
        btnLeft.setOnClickListener { moveRight() }
    }

    private fun moveRight() {
        if (!isGameOver && playerColumn < COLS - 1) {
            playerColumn++
            updateUI()
        }
    }

    private fun moveLeft() {
        if (!isGameOver && playerColumn > 0) {
            playerColumn--
            updateUI()
        }
    }

    private fun startGameLoop() {
        gameRunnable = Runnable {
            if (isGameOver) return@Runnable

            countEvadedObstacles()
            moveDown(obstacleMatrix)
            moveDown(coinMatrix)
            generateNewObstacle()
            generateNewCoin()
            checkCoinCollection()
            checkCollision()
            distance += 10
            updateUI()

            if (!isGameOver) {
                gameHandler.postDelayed(gameRunnable, currentDelay)
            }
        }
        gameHandler.post(gameRunnable)
    }

    private fun countEvadedObstacles() {
        for (c in 0 until COLS) {
            if (obstacleMatrix[ROWS - 1][c] && c != playerColumn) {
                score += 5
            }
        }
    }

    private fun moveDown(matrix: Array<BooleanArray>) {
        for (r in ROWS - 1 downTo 1) {
            for (c in 0 until COLS) {
                matrix[r][c] = matrix[r - 1][c]
            }
        }
        for (c in 0 until COLS) {
            matrix[0][c] = false
        }
    }

    private fun generateNewObstacle() {
        val randomCol = Random.nextInt(COLS)
        if (lastStepHadObstacle) {
            if (Random.nextFloat() < 0.15f) {
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

    private fun generateNewCoin() {
        if (Random.nextFloat() < 0.20f) {
            val col = Random.nextInt(COLS)

            if (!obstacleMatrix[0][col]) {
                coinMatrix[0][col] = true
            }
        }
    }

    private fun checkCoinCollection() {
        if (coinMatrix[ROWS - 1][playerColumn]) {
            score += 30
            coinMatrix[ROWS - 1][playerColumn] = false
            playSound(coinSoundId)
        }
    }

    private fun checkCollision() {
        if (obstacleMatrix[ROWS - 1][playerColumn]) {
            lives--
            obstacleMatrix[ROWS - 1][playerColumn] = false

            playSound(crashSoundId)
            vibrateOnCollision()

            if (lives <= 0) {
                isGameOver = true
                onGameOver()
            }
        }
    }

    private fun setupSensors() {
        if (mode == GameMode.SENSOR) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mode == GameMode.SENSOR) {
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || isGameOver || mode != GameMode.SENSOR) return

        val x = event.values[0]
        val y = event.values[1]

        val now = System.currentTimeMillis()
        if (now - lastTiltMoveTime > TILT_COOLDOWN) {
            if (x < -TILT_THRESHOLD) {
                moveLeft()
                lastTiltMoveTime = now
            } else if (x > TILT_THRESHOLD) {
                moveRight()
                lastTiltMoveTime = now
            }

        }

        val norm = (y / SensorManager.GRAVITY_EARTH).coerceIn(0.3f, 1.3f)
        currentDelay = (500L * norm).toLong().coerceIn(150L, 800L)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun setupSound() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()

        val crashRes = resources.getIdentifier("crash", "raw", packageName)
        if (crashRes != 0) crashSoundId = soundPool!!.load(this, crashRes, 1)

        val coinRes = resources.getIdentifier("coin", "raw", packageName)
        if (coinRes != 0) coinSoundId = soundPool!!.load(this, coinRes, 1)
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun vibrateOnCollision() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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

                when {
                    isPlayerPos -> {
                        imageView.setImageResource(R.drawable.student_player)
                        imageView.visibility = View.VISIBLE
                    }
                    obstacleMatrix[r][c] -> {
                        imageView.setImageResource(R.drawable.fail_obstacle)
                        imageView.visibility = View.VISIBLE
                    }
                    coinMatrix[r][c] -> {
                        if (coinDrawableRes != 0) {
                            imageView.setImageResource(coinDrawableRes)
                        } else {
                            imageView.setImageDrawable(null)
                        }
                        imageView.visibility = View.VISIBLE
                    }
                    else -> {
                        imageView.setImageDrawable(null)
                        imageView.visibility = View.INVISIBLE
                    }
                }
            }
        }

        txtScore.text = "🏆 נקודות: $score"
        txtLives.text = "❤️ חיים: $lives"
        txtDistance.text = "📏 מרחק: $distance מ'"
    }

    private fun onGameOver() {
        gameHandler.removeCallbacks(gameRunnable)
        saveScoreWithLocation()
    }

    private fun saveScoreWithLocation() {
        val finalScore = score
        val finalDistance = distance
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    persistScore(finalScore, finalDistance, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0)
                }
                .addOnFailureListener {
                    persistScore(finalScore, finalDistance, 0.0, 0.0)
                }
        } else {
            persistScore(finalScore, finalDistance, 0.0, 0.0)
        }
    }

    private fun persistScore(scoreVal: Int, distanceVal: Int, lat: Double, lng: Double) {
        ScoresManager.addScore(
            this,
            ScoreRecord(scoreVal, distanceVal, lat, lng, System.currentTimeMillis())
        )
        showGameOverDialog(scoreVal, distanceVal)
    }

    private fun showGameOverDialog(finalScore: Int, finalDistance: Int) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("נכשלת בקורס! 😵")
            .setMessage("צברת $finalScore נקודות זכות\nמרחק שעברת: $finalDistance מ'")
            .setCancelable(false)
            .setPositiveButton("שחק שוב") { _, _ -> restartGame() }
            .setNeutralButton("טבלת שיאים") { _, _ ->
                startActivity(Intent(this, HighScoresActivity::class.java))
                finish()
            }
            .setNegativeButton("תפריט") { _, _ -> finish() }
            .show()
    }

    private fun restartGame() {
        val restartIntent = intent
        finish()
        startActivity(restartIntent)
    }

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameHandler.removeCallbacks(gameRunnable)
        sensorManager?.unregisterListener(this)
        soundPool?.release()
        soundPool = null
    }
}