package com.tracker.bustracker.presentation.search

import com.tracker.bustracker.domain.model.StopPoint

enum class LocationField { ORIGIN, DESTINATION }

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class DisambiguationRequired(
        val field: LocationField,
        val options: List<StopPoint>
    ) : SearchUiState

    data class Error(val message: String) : SearchUiState
}
