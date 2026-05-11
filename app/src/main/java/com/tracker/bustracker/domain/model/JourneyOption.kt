package com.tracker.bustracker.domain.model

data class JourneyOption(
    val duration: Int,
    val legs: List<BusLeg>
)

data class BusLeg(
    val lineId: String,
    val lineName: String,
    val duration: Int,
    val summary: String,
    val departureStop: String,
    val arrivalStop: String
)
