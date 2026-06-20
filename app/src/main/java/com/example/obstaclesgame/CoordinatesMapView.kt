package com.example.obstaclesgame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Locale

class CoordinatesMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var points: List<ScoreRecord> = emptyList()
    private var selected: ScoreRecord? = null

    var onPointClick: ((ScoreRecord) -> Unit)? = null

    private val bgPaint = Paint().apply { color = Color.parseColor("#1E1E1E") }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#03DAC5") }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5252") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        textSize = 28f
    }

    private val padding = 70f
    private val screenPositions = ArrayList<Pair<ScoreRecord, FloatArray>>()

    fun setData(scores: List<ScoreRecord>) {
        points = scores.filter { !(it.latitude == 0.0 && it.longitude == 0.0) }
        invalidate()
    }

    fun setSelected(record: ScoreRecord?) {
        selected = record
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        screenPositions.clear()

        if (points.isEmpty()) {
            canvas.drawText("אין מיקומים להצגה", padding, height / 2f, hintPaint)
            return
        }

        var minLat = points.minOf { it.latitude }
        var maxLat = points.maxOf { it.latitude }
        var minLng = points.minOf { it.longitude }
        var maxLng = points.maxOf { it.longitude }

        if (maxLat - minLat < 1e-6) { minLat -= 0.001; maxLat += 0.001 }
        if (maxLng - minLng < 1e-6) { minLng -= 0.001; maxLng += 0.001 }

        val w = width - 2 * padding
        val h = height - 2 * padding

        var selectedPos: FloatArray? = null

        for (s in points) {
            val x = padding + ((s.longitude - minLng) / (maxLng - minLng) * w).toFloat()
            val y = padding + ((maxLat - s.latitude) / (maxLat - minLat) * h).toFloat()
            screenPositions.add(s to floatArrayOf(x, y))

            if (s == selected) {
                selectedPos = floatArrayOf(x, y)
            } else {
                canvas.drawCircle(x, y, 14f, dotPaint)
            }
        }

        selectedPos?.let { canvas.drawCircle(it[0], it[1], 22f, selectedPaint) }

        selected?.let {
            val info = String.format(
                Locale.US,
                "🏆 %d   (%.4f, %.4f)",
                it.score, it.latitude, it.longitude
            )
            canvas.drawText(info, padding, height - padding / 2, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tx = event.x
            val ty = event.y
            var best: ScoreRecord? = null
            var bestDist = Float.MAX_VALUE
            for ((rec, pos) in screenPositions) {
                val dx = pos[0] - tx
                val dy = pos[1] - ty
                val d = dx * dx + dy * dy
                if (d < bestDist) {
                    bestDist = d
                    best = rec
                }
            }
            if (best != null && bestDist < 90f * 90f) {
                onPointClick?.invoke(best)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
