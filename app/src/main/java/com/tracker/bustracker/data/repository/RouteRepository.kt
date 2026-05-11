package com.tracker.bustracker.data.repository

import com.tracker.bustracker.data.remote.TflApiService
import com.tracker.bustracker.data.remote.dto.RouteSequenceResponse

class RouteRepository(private val api: TflApiService) {

    private val cache = mutableMapOf<String, RouteSequenceResponse>()

    suspend fun getRouteSequence(lineId: String, direction: String = "outbound"): RouteSequenceResponse {
        val key = "$lineId-$direction"
        return cache.getOrPut(key) {
            api.getRouteSequence(lineId, direction)
        }
    }
}
