package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.remote.dto.ArrivalPredictionDto
import com.tracker.bustracker.data.repository.RouteRepository
import com.tracker.bustracker.domain.model.BusPosition
import com.tracker.bustracker.domain.model.LatLonPoint
import com.tracker.bustracker.domain.model.RouteStop
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
import timber.log.Timber

class ResolveBusPositionsUseCase(
    private val routeRepository: RouteRepository,
    private val json: Json
) {

    private var cachedStops: Map<String, RouteStop>? = null
    private var cachedPathPoints: List<LatLonPoint> = emptyList()

    val pathPoints: List<LatLonPoint> get() = cachedPathPoints

    suspend fun getRouteStops(lineId: String): List<RouteStop> {
        val response = routeRepository.getRouteSequence(lineId)
        val stops = response.stopPointSequences
            .flatMap { it.stopPoint }
            .map { dto ->
                RouteStop(
                    naptanId = dto.naptanId.ifEmpty { dto.id },
                    name = dto.name,
                    lat = dto.lat,
                    lon = dto.lon
                )
            }
            .distinctBy { it.naptanId }

        val stopMap = mutableMapOf<String, RouteStop>()
        for (stop in stops) {
            stopMap[stop.naptanId] = stop
        }
        response.stopPointSequences
            .flatMap { it.stopPoint }
            .forEach { dto ->
                val stop = RouteStop(
                    naptanId = dto.naptanId.ifEmpty { dto.id },
                    name = dto.name,
                    lat = dto.lat,
                    lon = dto.lon
                )
                stopMap[dto.id] = stop
                if (dto.naptanId.isNotEmpty()) stopMap[dto.naptanId] = stop
                if (dto.stationId != null) stopMap[dto.stationId] = stop
            }

        val allLineStrings = response.lineStrings
        Timber.d("lineStrings count: ${allLineStrings.size}")
        allLineStrings.firstOrNull()?.let { Timber.d("lineStrings[0] sample: ${it.take(120)}") }
        cachedPathPoints = parseLineStrings(allLineStrings)
        Timber.d("Route stops loaded: ${stopMap.size} entries, ${cachedPathPoints.size} path points for line $lineId")
        cachedStops = stopMap
        return stops
    }

    private fun parseLineStrings(lineStrings: List<String>): List<LatLonPoint> {
        return lineStrings.flatMap { lineString ->
            try {
                // TfL format is [[[lon, lat], [lon, lat], ...]] — one extra outer array
                val coordList = json.parseToJsonElement(lineString).jsonArray[0].jsonArray
                coordList.map { coordPair ->
                    val pair = coordPair.jsonArray
                    LatLonPoint(
                        lat = pair[1].jsonPrimitive.double,
                        lon = pair[0].jsonPrimitive.double
                    )
                }
            } catch (e: Exception) {
                Timber.w("Failed to parse lineString: ${e.message}")
                emptyList()
            }
        }
    }

    fun resolve(arrivals: List<ArrivalPredictionDto>): List<BusPosition> {
        val stopMap = cachedStops ?: return emptyList()

        val grouped = arrivals.groupBy { it.vehicleId }
        Timber.d("Resolving ${arrivals.size} arrivals for ${grouped.size} vehicles")

        return grouped
            .mapNotNull { (vehicleId, predictions) ->
                val nearest = predictions.minByOrNull { it.timeToStation } ?: return@mapNotNull null
                val stop = stopMap[nearest.naptanId]
                if (stop == null) {
                    Timber.w("No stop match for naptanId=${nearest.naptanId} (${nearest.stationName})")
                    return@mapNotNull null
                }
                BusPosition(
                    vehicleId = vehicleId,
                    lat = stop.lat,
                    lon = stop.lon,
                    nextStopName = stop.name,
                    timeToStation = nearest.timeToStation
                )
            }
            .sortedBy { it.timeToStation }
    }
}
