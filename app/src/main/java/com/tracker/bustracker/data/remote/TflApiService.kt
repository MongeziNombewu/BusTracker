package com.tracker.bustracker.data.remote

import com.tracker.bustracker.data.remote.dto.ArrivalPredictionDto
import com.tracker.bustracker.data.remote.dto.JourneyResultsResponse
import com.tracker.bustracker.data.remote.dto.RouteSequenceResponse
import com.tracker.bustracker.data.remote.dto.StopPointSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TflApiService {

    /**
     * Searches for bus stops matching the given query string.
     *
     * @param query The stop name or partial name to search for.
     * @param modes Comma-separated transport modes to filter by. Defaults to `"bus"`.
     * @return A [StopPointSearchResponse] containing a list of matching stops.
     */
    @GET("StopPoint/Search/{query}")
    suspend fun searchStopPoints(
        @Path("query") query: String,
        @Query("modes") modes: String = "bus"
    ): StopPointSearchResponse

    /**
     * Plans a journey between two locations and returns available bus routes.
     *
     * @param from Origin location as a `naptanId` or free-text string.
     * @param to Destination location as a `naptanId` or free-text string.
     * @return A [Response] wrapping [JourneyResultsResponse], which contains either a
     * list of journey options (HTTP 200) or disambiguation choices (HTTP 300).
     */
    @GET("Journey/JourneyResults/{from}/to/{to}")
    suspend fun getJourneyResults(
        @Path("from") from: String,
        @Path("to") to: String
    ): Response<JourneyResultsResponse>

    /**
     * Fetches live arrival predictions for all active buses on a given line.
     *
     * @param lineId The TfL line identifier.
     * @return A list of [ArrivalPredictionDto] for every bus currently running on the line.
     */
    @GET("Line/{lineId}/Arrivals")
    suspend fun getArrivals(
        @Path("lineId") lineId: String
    ): List<ArrivalPredictionDto>

    /**
     * Retrieves the ordered stop sequence and road geometry for a given line and direction.
     *
     * @param lineId The TfL line identifier.
     * @param direction The direction of travel. Defaults to `"outbound"`.
     * @return A [RouteSequenceResponse] containing the stop sequence and road geometry.
     */
    @GET("Line/{lineId}/Route/Sequence/{direction}")
    suspend fun getRouteSequence(
        @Path("lineId") lineId: String,
        @Path("direction") direction: String = "outbound"
    ): RouteSequenceResponse
}
