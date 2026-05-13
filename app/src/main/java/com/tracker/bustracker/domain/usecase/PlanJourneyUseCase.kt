package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.remote.dto.DisambiguationResult
import com.tracker.bustracker.data.repository.JourneyRepository
import com.tracker.bustracker.domain.model.BusLeg
import com.tracker.bustracker.domain.model.JourneyOption
import com.tracker.bustracker.domain.model.JourneyPlanResult
import com.tracker.bustracker.domain.model.StopPoint

class PlanJourneyUseCase(private val repository: JourneyRepository) {

    suspend operator fun invoke(from: String, to: String): JourneyPlanResult {
        val response = repository.getJourneyResults(from, to)

        val fromNeedsDisambiguation = response.fromLocationDisambiguation.needsDisambiguation()
        val toNeedsDisambiguation = response.toLocationDisambiguation.needsDisambiguation()

        if (fromNeedsDisambiguation || toNeedsDisambiguation) {
            return JourneyPlanResult.NeedsDisambiguation(
                fromOptions = response.fromLocationDisambiguation?.toStopPoints().orEmpty(),
                toOptions = response.toLocationDisambiguation?.toStopPoints().orEmpty()
            )
        }

        val journeys = response.journeys?.map { journey ->
            val busLegs = journey.legs
                .filter { it.mode.id == "bus" }
                .map { leg ->
                    val routeOption = leg.routeOptions.firstOrNull()
                    BusLeg(
                        lineId = routeOption?.lineIdentifier?.id.orEmpty(),
                        lineName = routeOption?.lineIdentifier?.name ?: routeOption?.name.orEmpty(),
                        duration = leg.duration,
                        summary = leg.instruction.summary,
                        departureStop = leg.departurePoint?.commonName.orEmpty(),
                        arrivalStop = leg.arrivalPoint?.commonName.orEmpty()
                    )
                }
            JourneyOption(duration = journey.duration, legs = busLegs)
        } ?: emptyList()

        return JourneyPlanResult.Success(journeys)
    }

    private fun DisambiguationResult?.needsDisambiguation(): Boolean {
        return this?.matchStatus?.equals("list", ignoreCase = true) == true
                || (this?.disambiguationOptions?.size ?: 0) > 1
    }

    private fun DisambiguationResult.toStopPoints(): List<StopPoint> {
        return disambiguationOptions.map { option ->
            val place = option.place
            StopPoint(
                id = option.parameterValue,
                name = place.commonName,
                lat = place.lat,
                lon = place.lon
            )
        }
    }
}
