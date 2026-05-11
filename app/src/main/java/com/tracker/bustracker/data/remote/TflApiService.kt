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
     * Calls `StopPoint/Search/{query}?modes=bus`. Results are filtered to bus stops only
     * via the [modes] parameter, excluding tube, rail, and other transport types.
     *
     * @param query The stop name or partial name to search for (e.g. "Brixton").
     * @param modes Comma-separated transport modes to filter by. Defaults to `"bus"`.
     * @return A [StopPointSearchResponse] containing a list of matching stops, each with
     * a `naptanId`, display name, and coordinates.
     */
    @GET("StopPoint/Search/{query}")
    suspend fun searchStopPoints(
        @Path("query") query: String,
        @Query("modes") modes: String = "bus"
    ): StopPointSearchResponse

    /**
     * Plans a journey between two locations and returns available bus routes.
     *
     * Calls `Journey/JourneyResults/{from}/to/{to}`. Both [from] and [to] can be
     * `naptanId` values from a prior stop search or free-text location strings.
     *
     * The response is wrapped in [Response] because TfL returns **HTTP 300** (not 200)
     * when either location is ambiguous. In that case the 300 body contains a
     * disambiguation list for the user to resolve. The repository inspects the status
     * code and parses the error body manually when a 300 is received.
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
     * Calls `Line/{lineId}/Arrivals`. Returns every prediction across all stops on the
     * line at the time of the request. Each [ArrivalPredictionDto] includes the vehicle
     * ID, the `naptanId` of the next stop, the time to station in seconds, and the stop
     * name. The app groups predictions by `vehicleId` and takes the nearest stop per bus
     * to infer its current position on the map.
     *
     * This endpoint is polled every 30 seconds on the tracking screen to keep bus
     * positions up to date.
     *
     * @param lineId The TfL line identifier (e.g. `"405"`).
     * @return A list of [ArrivalPredictionDto] for every bus currently running on the line.
     */
    @GET("Line/{lineId}/Arrivals")
    suspend fun getArrivals(
        @Path("lineId") lineId: String
    ): List<ArrivalPredictionDto>

    /**
     * Retrieves the ordered stop sequence and road geometry for a given line and direction.
     *
     * Calls `Line/{lineId}/Route/Sequence/{direction}`. Fetched once when the tracking
     * screen loads and cached in memory. The response serves two purposes:
     * - **Stop map**: the ordered list of stops with `naptanId` and coordinates is used
     *   to resolve arrival predictions to map positions (Virtual GPS).
     * - **Route polyline**: the top-level `lineStrings` field contains the road-accurate
     *   path as arrays of `[lon, lat]` coordinate pairs, used to draw the route on the map.
     *
     * @param lineId The TfL line identifier (e.g. `"405"`).
     * @param direction The direction of travel. Defaults to `"outbound"`.
     * @return A [RouteSequenceResponse] containing the stop sequence and road geometry.
     */
    @GET("Line/{lineId}/Route/Sequence/{direction}")
    suspend fun getRouteSequence(
        @Path("lineId") lineId: String,
        @Path("direction") direction: String = "outbound"
    ): RouteSequenceResponse
}
