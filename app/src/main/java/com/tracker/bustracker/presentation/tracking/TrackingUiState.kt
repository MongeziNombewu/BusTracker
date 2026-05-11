package com.tracker.bustracker.presentation.tracking

import com.tracker.bustracker.domain.model.BusPosition
import com.tracker.bustracker.domain.model.LatLonPoint
import com.tracker.bustracker.domain.model.RouteStop

sealed interface TrackingUiState {
    data object Loading : TrackingUiState

    data class Tracking(
        val lineName: String,
        val buses: List<BusPosition>,
        val routeStops: List<RouteStop>,
        val routePath: List<LatLonPoint> = emptyList()
    ) : TrackingUiState

    data object NoBusesAvailable : TrackingUiState

    data class Error(val message: String) : TrackingUiState
}
