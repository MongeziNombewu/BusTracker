package com.tracker.bustracker.domain.model

data class BusPosition(
    val vehicleId: String,
    val lat: Double,
    val lon: Double,
    val nextStopName: String,
    val timeToStation: Int
)
