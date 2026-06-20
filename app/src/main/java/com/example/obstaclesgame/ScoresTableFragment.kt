package com.example.obstaclesgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScoresTableFragment : Fragment() {

    private val viewModel: ScoresViewModel by activityViewModels()
    private lateinit var adapter: ScoresAdapter
    private lateinit var txtEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_scores_table, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtEmpty = view.findViewById(R.id.txtEmpty)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerScores)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = ScoresAdapter(emptyList()) { record ->
            viewModel.selectScore(record)
        }
        recycler.adapter = adapter

        viewModel.scores.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
            txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
