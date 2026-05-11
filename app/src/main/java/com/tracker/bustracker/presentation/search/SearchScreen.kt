package com.tracker.bustracker.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.bustracker.R
import com.tracker.bustracker.domain.model.StopPoint
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onJourneyReady: (from: String, to: String, fromName: String, toName: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val originText by viewModel.originText.collectAsStateWithLifecycle()
    val destinationText by viewModel.destinationText.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToJourneyResults -> {
                    onJourneyReady(event.from, event.to, event.fromName, event.toName)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.search_journey)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = originText,
                onValueChange = viewModel::onOriginChanged,
                label = { Text(stringResource(R.string.from)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = destinationText,
                onValueChange = viewModel::onDestinationChanged,
                label = { Text(stringResource(R.string.to)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::onSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = originText.isNotBlank() && destinationText.isNotBlank()
                        && uiState !is SearchUiState.Loading
            ) {
                Text(stringResource(R.string.search))
            }

            when (val state = uiState) {
                is SearchUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                is SearchUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {}
            }

            if (uiState is SearchUiState.DisambiguationRequired) {
                val state = uiState as SearchUiState.DisambiguationRequired
                DisambiguationSheet(
                    field = state.field,
                    options = state.options,
                    origin = originText,
                    destination = destinationText,
                    onSelected = viewModel::onDisambiguationSelected,
                    onDismiss = viewModel::onDismissDisambiguation
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisambiguationSheet(
    field: LocationField,
    options: List<StopPoint>,
    origin: String,
    destination: String,
    onSelected: (StopPoint) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val title = when (field) {
        LocationField.ORIGIN -> stringResource(R.string.select_origin)
        LocationField.DESTINATION -> stringResource(R.string.select_destination)
    }

    val description = when (field) {
        LocationField.ORIGIN -> stringResource(R.string.multiple_matches_found_for, origin)
        LocationField.DESTINATION -> stringResource(R.string.multiple_matches_found_for, destination)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(options) { stop ->
                    Text(
                        text = stop.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(stop) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
