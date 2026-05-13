package com.tracker.bustracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RouteSequenceResponse(
    val lineId: String = "",
    val lineName: String = "",
    val direction: String = "",
    val lineStrings: List<String> = emptyList(),
    val stopPointSequences: List<StopPointSequenceDto> = emptyList()
)

@Serializable
data class StopPointSequenceDto(
    val direction: String = "",
    val branchId: Int = 0,
    val stopPoint: List<RouteStopDto> = emptyList()
)

@Serializable
data class RouteStopDto(
    val id: String = "",
    val naptanId: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val stationId: String = "",
    val topMostParentId: String = ""
)
