package com.tracker.bustracker.presentation.journeyresults

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.bustracker.R
import com.tracker.bustracker.domain.model.BusLeg
import com.tracker.bustracker.domain.model.StopPoint
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyResultsScreen(
    from: String,
    to: String,
    fromName: String,
    toName: String,
    onLegSelected: (lineId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: JourneyResultsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(from, to) {
        viewModel.loadJourney(from, to)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journey_options)) },
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
                is JourneyResultsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is JourneyResultsUiState.NoBusRoutes -> {
                    Text(
                        text = stringResource(R.string.no_bus_routes_found),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                is JourneyResultsUiState.Results -> {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.journey_from_to, fromName, toName),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        state.journeys.forEach { journey ->
                            items(journey.legs) { leg ->
                                BusLegCard(leg = leg, onClick = { onLegSelected(leg.lineId) })
                            }
                        }
                    }
                }

                is JourneyResultsUiState.JourneyDisambiguation -> {
                    DisambiguationList(
                        fromOptions = state.fromOptions,
                        toOptions = state.toOptions,
                        onResolved = viewModel::onDisambiguationResolved
                    )
                }

                is JourneyResultsUiState.Error -> {
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
private fun BusLegCard(leg: BusLeg, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.route, leg.lineName),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.duration_min, leg.duration),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = leg.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${leg.departureStop} → ${leg.arrivalStop}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DisambiguationList(
    fromOptions: List<StopPoint>?,
    toOptions: List<StopPoint>?,
    onResolved: (from: StopPoint?, to: StopPoint?) -> Unit
) {
    // I can't replicate the flow again, come back to this
    var selectedFrom by remember { mutableStateOf<StopPoint?>(null) }
    val options = fromOptions ?: toOptions ?: return
    val title = if (fromOptions != null) {
        stringResource(R.string.select_origin)
    } else {
        stringResource(R.string.select_destination)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(options) { stop ->
                Text(
                    text = stop.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (fromOptions != null) {
                                if (toOptions != null) {
                                    selectedFrom = stop
                                } else {
                                    onResolved(stop, null)
                                }
                            } else {
                                onResolved(selectedFrom, stop)
                            }
                        }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                HorizontalDivider()
            }
        }
    }
}
