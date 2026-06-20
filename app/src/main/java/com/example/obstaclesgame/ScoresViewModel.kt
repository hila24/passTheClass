package com.example.obstaclesgame

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class ScoresViewModel(application: Application) : AndroidViewModel(application) {

    val scores = MutableLiveData<List<ScoreRecord>>()
    val selectedScore = MutableLiveData<ScoreRecord?>()

    fun loadScores() {
        scores.value = ScoresManager.getScores(getApplication())
    }

    fun selectScore(record: ScoreRecord) {
        selectedScore.value = record
    }
}
