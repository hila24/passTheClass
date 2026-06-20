package com.example.obstaclesgame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class ScoresMapFragment : Fragment() {

    private val viewModel: ScoresViewModel by activityViewModels()
    private lateinit var mapView: CoordinatesMapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scores_map, container, false)
        mapView = view.findViewById(R.id.coordinatesMap)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.onPointClick = { record ->
            viewModel.selectScore(record)
        }

        viewModel.scores.observe(viewLifecycleOwner) { list ->
            mapView.setData(list)
        }
        viewModel.selectedScore.observe(viewLifecycleOwner) { record ->
            mapView.setSelected(record)
        }
    }
}
