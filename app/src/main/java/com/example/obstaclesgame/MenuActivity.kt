package com.example.obstaclesgame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        findViewById<Button>(R.id.btnSlow).setOnClickListener {
            startGame(GameMode.BUTTONS_SLOW)
        }
        findViewById<Button>(R.id.btnFast).setOnClickListener {
            startGame(GameMode.BUTTONS_FAST)
        }
        findViewById<Button>(R.id.btnSensor).setOnClickListener {
            startGame(GameMode.SENSOR)
        }
        findViewById<Button>(R.id.btnHighScores).setOnClickListener {
            startActivity(Intent(this, HighScoresActivity::class.java))
        }
    }

    private fun startGame(mode: GameMode) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_MODE, mode.name)
        startActivity(intent)
    }
}
