package com.tracker.bustracker.presentation.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.bustracker.domain.usecase.GetLiveArrivalsUseCase
import com.tracker.bustracker.domain.usecase.ResolveBusPositionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(
    private val lineId: String,
    private val getLiveArrivals: GetLiveArrivalsUseCase,
    private val resolveBusPositions: ResolveBusPositionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrackingUiState>(TrackingUiState.Loading)
    val uiState: StateFlow<TrackingUiState> = _uiState.onStart {
        startTracking()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingUiState.Loading
    )

    private fun startTracking() {
        viewModelScope.launch {
            try {
                val routeStops = resolveBusPositions.getRouteStops(lineId)

                getLiveArrivals(lineId)
                    .catch { e ->
                        _uiState.value = TrackingUiState.Error(e.message.orEmpty())
                    }
                    .collect { arrivals ->
                        val positions = resolveBusPositions.resolve(arrivals)
                        _uiState.value = if (positions.isEmpty()) {
                            TrackingUiState.NoBusesAvailable
                        } else {
                            TrackingUiState.Tracking(
                                lineName = lineId,
                                buses = positions,
                                routeStops = routeStops,
                                routePath = resolveBusPositions.pathPoints
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = TrackingUiState.Error(e.message.orEmpty())
            }
        }
    }
}
