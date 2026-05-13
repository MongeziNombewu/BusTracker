package com.tracker.bustracker.domain.model

sealed interface JourneyPlanResult {
    data class Success(val journeys: List<JourneyOption>) : JourneyPlanResult
    data class NeedsDisambiguation(
        val fromOptions: List<StopPoint>,
        val toOptions: List<StopPoint>
    ) : JourneyPlanResult
}
