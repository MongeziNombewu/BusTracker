package com.tracker.bustracker.presentation.journeyresults

import com.tracker.bustracker.domain.model.JourneyOption
import com.tracker.bustracker.domain.model.StopPoint

sealed interface JourneyResultsUiState {
    data object Loading : JourneyResultsUiState

    data class Results(val journeys: List<JourneyOption>) : JourneyResultsUiState

    data object NoBusRoutes : JourneyResultsUiState

    data class JourneyDisambiguation(
        val fromOptions: List<StopPoint>?,
        val toOptions: List<StopPoint>?
    ) : JourneyResultsUiState

    data class Error(val message: String) : JourneyResultsUiState
}
