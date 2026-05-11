package com.tracker.bustracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class JourneyResultsResponse(
    val journeys: List<JourneyDto>? = null,
    val toLocationDisambiguation: DisambiguationResult? = null,
    val fromLocationDisambiguation: DisambiguationResult? = null
)

@Serializable
data class DisambiguationResult(
    val disambiguationOptions: List<DisambiguationOption>? = null,
    val matchStatus: String? = null
)

@Serializable
data class DisambiguationOption(
    val parameterValue: String? = null,
    val place: PlaceDto? = null
)

@Serializable
data class PlaceDto(
    val commonName: String? = null,
    val placeType: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

@Serializable
data class JourneyDto(
    val duration: Int = 0,
    val legs: List<LegDto> = emptyList()
)

@Serializable
data class LegDto(
    val duration: Int = 0,
    val instruction: InstructionDto? = null,
    val mode: ModeDto? = null,
    val routeOptions: List<RouteOptionDto> = emptyList(),
    val departurePoint: PointDto? = null,
    val arrivalPoint: PointDto? = null
)

@Serializable
data class InstructionDto(
    val summary: String? = null,
    val detailed: String? = null
)

@Serializable
data class ModeDto(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class RouteOptionDto(
    val name: String? = null,
    val lineIdentifier: LineIdentifierDto? = null
)

@Serializable
data class LineIdentifierDto(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class PointDto(
    val commonName: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
