package com.tracker.bustracker.presentation.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.bustracker.domain.usecase.GetLiveArrivalsUseCase
import com.tracker.bustracker.domain.usecase.ResolveBusPositionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

class TrackingViewModel(
    private val lineId: String,
    private val getLiveArrivals: GetLiveArrivalsUseCase,
    private val resolveBusPositions: ResolveBusPositionsUseCase
) : ViewModel() {

    val uiState: StateFlow<TrackingUiState> = trackingFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = TrackingUiState.Loading
        )

    private fun trackingFlow(): Flow<TrackingUiState> = flow {
        val routeStops = resolveBusPositions.getRouteStops(lineId)
        getLiveArrivals(lineId).collect { arrivals ->
            val positions = resolveBusPositions.resolve(arrivals)
            emit(
                if (positions.isEmpty()) {
                    TrackingUiState.NoBusesAvailable
                } else {
                    TrackingUiState.Tracking(
                        lineName = lineId,
                        buses = positions,
                        routeStops = routeStops,
                        routePath = resolveBusPositions.pathPoints
                    )
                }
            )
        }
    }.catch { e ->
        Timber.e(e, "Tracking failed for lineId=%s", lineId)
        emit(TrackingUiState.Error(e.message.orEmpty()))
    }
}
