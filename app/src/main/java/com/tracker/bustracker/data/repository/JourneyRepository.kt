package com.tracker.bustracker.data.repository

import com.tracker.bustracker.data.remote.TflApiService
import com.tracker.bustracker.data.remote.dto.JourneyResultsResponse
import com.tracker.bustracker.data.remote.dto.StopPointSearchResponse
import kotlinx.serialization.json.Json

class JourneyRepository(
    private val api: TflApiService,
    private val json: Json
) {

    suspend fun searchStopPoints(query: String): StopPointSearchResponse {
        return api.searchStopPoints(query)
    }

    suspend fun getJourneyResults(from: String, to: String): JourneyResultsResponse {
        val response = api.getJourneyResults(from, to)

        if (response.isSuccessful) {
            return response.body() ?: throw IllegalStateException("Empty response body")
        }

        // TfL returns 300 for disambiguation — parse the error body as the same DTO
        if (response.code() == 300) {
            val errorBody = response.errorBody()?.string()
                ?: throw IllegalStateException("Empty disambiguation response")
            return json.decodeFromString<JourneyResultsResponse>(errorBody)
        }

        throw IllegalStateException("Journey API error: ${response.code()}")
    }
}
