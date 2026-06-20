package com.example.obstaclesgame

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class HighScoresActivity : AppCompatActivity() {

    private val viewModel: ScoresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        viewModel.loadScores()

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
