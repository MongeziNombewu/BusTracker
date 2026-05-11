package com.tracker.bustracker.data.repository

import com.tracker.bustracker.data.remote.TflApiService
import com.tracker.bustracker.data.remote.dto.ArrivalPredictionDto

class ArrivalsRepository(private val api: TflApiService) {

    suspend fun getArrivals(lineId: String): List<ArrivalPredictionDto> {
        return api.getArrivals(lineId)
    }
}
