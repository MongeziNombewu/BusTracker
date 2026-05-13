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
    val disambiguationOptions: List<DisambiguationOption> = emptyList(),
    val matchStatus: String = ""
)

@Serializable
data class DisambiguationOption(
    val parameterValue: String,
    val place: PlaceDto
)

@Serializable
data class PlaceDto(
    val commonName: String = "",
    val placeType: String = "",
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
    val instruction: InstructionDto,
    val mode: ModeDto,
    val routeOptions: List<RouteOptionDto> = emptyList(),
    val departurePoint: PointDto? = null,
    val arrivalPoint: PointDto? = null
)

@Serializable
data class InstructionDto(
    val summary: String = "",
    val detailed: String = ""
)

@Serializable
data class ModeDto(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class RouteOptionDto(
    val name: String = "",
    val lineIdentifier: LineIdentifierDto? = null
)

@Serializable
data class LineIdentifierDto(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class PointDto(
    val commonName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
