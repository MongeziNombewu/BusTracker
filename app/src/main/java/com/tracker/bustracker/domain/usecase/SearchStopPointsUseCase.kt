package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.repository.JourneyRepository
import com.tracker.bustracker.domain.model.StopPoint

class SearchStopPointsUseCase(private val repository: JourneyRepository) {

    suspend operator fun invoke(query: String): List<StopPoint> {
        val response = repository.searchStopPoints(query)
        return response.matches.map { match ->
            StopPoint(
                id = match.id,
                name = match.name,
                lat = match.lat,
                lon = match.lon
            )
        }
    }
}
