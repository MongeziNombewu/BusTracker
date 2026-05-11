package com.tracker.bustracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArrivalPredictionDto(
    val vehicleId: String = "",
    val naptanId: String = "",
    val stationName: String = "",
    val lineId: String = "",
    val lineName: String = "",
    val destinationName: String = "",
    val timeToStation: Int = 0,
    val currentDirection: String? = null
)
