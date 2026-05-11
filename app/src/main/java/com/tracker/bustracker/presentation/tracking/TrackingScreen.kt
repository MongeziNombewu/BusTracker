package com.tracker.bustracker.presentation.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.tracker.bustracker.R
import com.tracker.bustracker.domain.model.BusPosition
import com.tracker.bustracker.domain.model.LatLonPoint
import com.tracker.bustracker.domain.model.RouteStop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    onBack: () -> Unit,
    viewModel: TrackingViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.real_time_map)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is TrackingUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is TrackingUiState.Tracking -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        BusMap(
                            buses = state.buses,
                            routeStops = state.routeStops,
                            routePath = state.routePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        BusList(
                            lineName = state.lineName,
                            buses = state.buses,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                is TrackingUiState.NoBusesAvailable -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.location_on),
                            contentDescription = "No buses",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_buses_running),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.no_active_buses_at_this_time),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is TrackingUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BusMap(
    buses: List<BusPosition>,
    routeStops: List<RouteStop>,
    routePath: List<LatLonPoint>,
    modifier: Modifier = Modifier
) {
    val center = buses.firstOrNull()?.let { LatLng(it.lat, it.lon) }
        ?: routeStops.firstOrNull()?.let { LatLng(it.lat, it.lon) }
        ?: LatLng(51.5074, -0.1278)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 13f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        val busIcon = BitmapDescriptorFactory.fromResource(R.drawable.directions_bus)
        val stopIcon = BitmapDescriptorFactory.fromResource(R.drawable.trip_origin)
        val pathLatLngs = routePath.map { LatLng(it.lat, it.lon) }
        if (pathLatLngs.size >= 2) {
            Polyline(
                points = pathLatLngs,
                color = Color(0xFF1976D2),
                width = 10f
            )
        }

        routeStops.forEach { stop ->
            val markerState = rememberUpdatedMarkerState(
                position = LatLng(stop.lat, stop.lon)
            )
            Marker(
                state = markerState,
                title = stop.name,
                icon = stopIcon
            )
        }

        buses.forEach { bus ->
            val markerState = rememberUpdatedMarkerState(
                position = LatLng(bus.lat, bus.lon)
            )
            Marker(
                state = markerState,
                title = stringResource(R.string.bus_name, bus.vehicleId),
                snippet = stringResource(R.string.bus_marker_hint, bus.nextStopName, bus.timeToStation / 60),
                icon = busIcon
            )
        }
    }
}

@Composable
private fun BusList(
    lineName: String,
    buses: List<BusPosition>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.buses_on_route, lineName),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(buses) { bus ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.location_on),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.bus_name, bus.vehicleId),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.arriving_at, bus.nextStopName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.duration_min, bus.timeToStation / 60),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
