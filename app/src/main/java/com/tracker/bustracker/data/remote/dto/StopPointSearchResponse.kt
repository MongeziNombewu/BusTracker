package com.tracker.bustracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StopPointSearchResponse(
    val total: Int = 0,
    val matches: List<StopPointMatch> = emptyList()
)

@Serializable
data class StopPointMatch(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
