package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.remote.dto.ArrivalPredictionDto
import com.tracker.bustracker.data.repository.RouteRepository
import com.tracker.bustracker.domain.model.BusPosition
import com.tracker.bustracker.domain.model.LatLonPoint
import com.tracker.bustracker.domain.model.RouteStop
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

class ResolveBusPositionsUseCase(
    private val routeRepository: RouteRepository,
    private val json: Json
) {
    private var cachedStops: List<RouteStop> = emptyList()
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

        val allLineStrings = response.lineStrings
        cachedPathPoints = parseLineStrings(allLineStrings)
        cachedStops = stops

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
        if (cachedStops.isEmpty()) return emptyList()
        val stopMap: Map<String, RouteStop> = cachedStops.associateBy { it.naptanId }

        val grouped = arrivals.groupBy { it.vehicleId }

        return grouped
            .mapNotNull { (vehicleId, predictions) ->
                val sorted = predictions.sortedBy { it.timeToStation }
                val nearest = sorted.first()
                val nearestStop = stopMap[nearest.naptanId] ?: run {
                    Timber.w("No stop match for naptanId=${nearest.naptanId} (${nearest.stationName})")
                    return@mapNotNull null
                }

                val position = estimatePosition(sorted, nearest, nearestStop)

                BusPosition(
                    vehicleId = vehicleId,
                    lat = position.lat,
                    lon = position.lon,
                    nextStopName = nearestStop.name,
                    timeToStation = nearest.timeToStation
                )
            }
            .sortedBy { it.timeToStation }
    }

    private fun estimatePosition(
        sorted: List<ArrivalPredictionDto>,
        nearest: ArrivalPredictionDto,
        nearestStop: RouteStop
    ): LatLonPoint {
        val second = sorted.getOrNull(1) ?: return LatLonPoint(nearestStop.lat, nearestStop.lon)

        val segmentDuration = second.timeToStation - nearest.timeToStation
        if (segmentDuration <= 0) return LatLonPoint(nearestStop.lat, nearestStop.lon)


        val nearestIndex = cachedStops.indexOfFirst { it.naptanId == nearestStop.naptanId }
        val previousStop = cachedStops.getOrNull(nearestIndex - 1)
            ?: return LatLonPoint(nearestStop.lat, nearestStop.lon)

        // https://www.geeksforgeeks.org/maths/linear-interpolation-formula/
        // fraction = (x - x0) / (x1 - x0)
        // I likely need to use linear interpolation to estimate the position between the two stops based on
        // timeToStation. Then use Pythagorean or Dijkstra theorem to calculate the lat/lon offset from the nearest point
        // and select the closest point on the path (cachedPathPoints) to snap to.
        // This is a bit complex, so for now I'll just return the nearest stop's position until I can implement this properly.

        return LatLonPoint(nearestStop.lat, nearestStop.lon)
    }
}
