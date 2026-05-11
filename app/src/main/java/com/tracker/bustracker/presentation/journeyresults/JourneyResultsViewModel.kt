package com.tracker.bustracker.presentation.journeyresults

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.bustracker.domain.model.JourneyPlanResult
import com.tracker.bustracker.domain.model.StopPoint
import com.tracker.bustracker.domain.usecase.PlanJourneyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JourneyResultsViewModel(
    private val planJourney: PlanJourneyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<JourneyResultsUiState>(JourneyResultsUiState.Loading)
    val uiState: StateFlow<JourneyResultsUiState> = _uiState.asStateFlow()

    private var currentFrom: String = ""
    private var currentTo: String = ""

    fun loadJourney(from: String, to: String) {
        currentFrom = from
        currentTo = to
        fetchJourney(from, to)
    }

    private fun fetchJourney(from: String, to: String) {
        viewModelScope.launch {
            _uiState.value = JourneyResultsUiState.Loading
            try {
                when (val result = planJourney(from, to)) {
                    is JourneyPlanResult.Success -> {
                        val busJourneys = result.journeys.filter { it.legs.isNotEmpty() }
                        if (busJourneys.isEmpty()) {
                            _uiState.value = JourneyResultsUiState.Error("No bus routes found")
                        } else {
                            _uiState.value = JourneyResultsUiState.Results(busJourneys)
                        }
                    }
                    is JourneyPlanResult.NeedsDisambiguation -> {
                        _uiState.value = JourneyResultsUiState.JourneyDisambiguation(
                            fromOptions = result.fromOptions,
                            toOptions = result.toOptions
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = JourneyResultsUiState.Error(e.message ?: "Failed to plan journey")
            }
        }
    }

    fun onDisambiguationResolved(from: StopPoint?, to: StopPoint?) {
        val resolvedFrom = from?.id ?: currentFrom
        val resolvedTo = to?.id ?: currentTo
        fetchJourney(resolvedFrom, resolvedTo)
    }
}
