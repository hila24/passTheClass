package com.example.obstaclesgame

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ScoresManager {

    private const val PREFS_NAME = "high_scores_prefs"
    private const val KEY_SCORES = "scores_list"
    private const val MAX_SCORES = 10

    private val gson = Gson()

    fun getScores(context: Context): List<ScoreRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SCORES, null) ?: return emptyList()
        val type = object : TypeToken<List<ScoreRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addScore(context: Context, record: ScoreRecord): Boolean {
        val current = getScores(context).toMutableList()
        current.add(record)
        current.sortByDescending { it.score }
        val trimmed = current.take(MAX_SCORES)
        save(context, trimmed)
        return trimmed.contains(record)
    }

    private fun save(context: Context, scores: List<ScoreRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SCORES, gson.toJson(scores)).apply()
    }
}
