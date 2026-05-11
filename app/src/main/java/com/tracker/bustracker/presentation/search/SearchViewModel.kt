package com.tracker.bustracker.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.bustracker.domain.model.StopPoint
import com.tracker.bustracker.domain.usecase.SearchStopPointsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface NavigationEvent {
    data class NavigateToJourneyResults(
        val from: String,
        val to: String,
        val fromName: String,
        val toName: String
    ) : NavigationEvent
}

class SearchViewModel(
    private val searchStopPoints: SearchStopPointsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _originText = MutableStateFlow("")
    val originText: StateFlow<String> = _originText.asStateFlow()

    private val _destinationText = MutableStateFlow("")
    val destinationText: StateFlow<String> = _destinationText.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var resolvedOrigin: StopPoint? = null
    private var resolvedDestination: StopPoint? = null

    private var pendingField: LocationField? = null

    fun onOriginChanged(text: String) {
        _originText.value = text
        resolvedOrigin = null
    }

    fun onDestinationChanged(text: String) {
        _destinationText.value = text
        resolvedDestination = null
    }

    fun onSearch() {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                if (resolvedOrigin == null) {
                    val results = searchStopPoints(_originText.value.trim())
                    when {
                        results.isEmpty() -> {
                            _uiState.value = SearchUiState.Error("No stops found for \"${_originText.value}\"")
                            return@launch
                        }
                        results.size == 1 -> resolvedOrigin = results.first()
                        else -> {
                            pendingField = LocationField.ORIGIN
                            _uiState.value = SearchUiState.DisambiguationRequired(
                                field = LocationField.ORIGIN,
                                options = results
                            )
                            return@launch
                        }
                    }
                }

                if (resolvedDestination == null) {
                    val results = searchStopPoints(_destinationText.value.trim())
                    when {
                        results.isEmpty() -> {
                            _uiState.value = SearchUiState.Error("No stops found for \"${_destinationText.value}\"")
                            return@launch
                        }
                        results.size == 1 -> resolvedDestination = results.first()
                        else -> {
                            pendingField = LocationField.DESTINATION
                            _uiState.value = SearchUiState.DisambiguationRequired(
                                field = LocationField.DESTINATION,
                                options = results
                            )
                            return@launch
                        }
                    }
                }

                val from = resolvedOrigin!!.id
                val to = resolvedDestination!!.id
                val fromName = resolvedOrigin!!.name
                val toName = resolvedDestination!!.name
                _uiState.value = SearchUiState.Idle
                _navigationEvent.send(NavigationEvent.NavigateToJourneyResults(from, to, fromName, toName))
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message.orEmpty())
            }
        }
    }

    fun onDismissDisambiguation() {
        _uiState.value = SearchUiState.Idle
        pendingField = null
    }

    fun onDisambiguationSelected(stopPoint: StopPoint) {
        when (pendingField) {
            LocationField.ORIGIN -> {
                resolvedOrigin = stopPoint
                _originText.value = stopPoint.name
            }
            LocationField.DESTINATION -> {
                resolvedDestination = stopPoint
                _destinationText.value = stopPoint.name
            }
            null -> return
        }
        pendingField = null
        onSearch()
    }
}
