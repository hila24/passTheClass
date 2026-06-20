package com.example.obstaclesgame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoresAdapter(
    private var items: List<ScoreRecord>,
    private val onClick: (ScoreRecord) -> Unit
) : RecyclerView.Adapter<ScoresAdapter.ScoreViewHolder>() {

    inner class ScoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtRank: TextView = view.findViewById(R.id.txtRank)
        val txtScore: TextView = view.findViewById(R.id.txtScoreItem)
        val txtDistance: TextView = view.findViewById(R.id.txtDistanceItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_score, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val record = items[position]
        holder.txtRank.text = "${position + 1}"
        holder.txtScore.text = "🏆 ${record.score}"
        holder.txtDistance.text = "📏 ${record.distance} מ'"
        holder.itemView.setOnClickListener { onClick(record) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ScoreRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}