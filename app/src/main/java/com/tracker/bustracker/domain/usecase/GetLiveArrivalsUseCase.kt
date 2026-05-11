package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.remote.dto.ArrivalPredictionDto
import com.tracker.bustracker.data.repository.ArrivalsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

class GetLiveArrivalsUseCase(private val repository: ArrivalsRepository) {

    operator fun invoke(lineId: String): Flow<List<ArrivalPredictionDto>> = flow {
        while (true) {
            val arrivals = repository.getArrivals(lineId)
            emit(arrivals)
            delay(30.seconds)
        }
    }
}
