package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.remote.dto.StopPointMatch
import com.tracker.bustracker.data.remote.dto.StopPointSearchResponse
import com.tracker.bustracker.data.repository.JourneyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchStopPointsUseCaseTest {

    private val repository: JourneyRepository = mockk()
    private lateinit var useCase: SearchStopPointsUseCase

    @Before
    fun setUp() {
        useCase = SearchStopPointsUseCase(repository)
    }

    @Test
    fun `maps response matches to StopPoint list`() = runTest {
        coEvery { repository.searchStopPoints(any()) } returns StopPointSearchResponse(
            total = 2,
            matches = listOf(
                StopPointMatch(id = "490000053A", name = "Victoria Station 1", lat = 51.4613, lon = -0.1156),
                StopPointMatch(id = "490000211S", name = "Victoria Station 2", lat = 51.4721, lon = -0.1224)
            )
        )

        val result = useCase("Victoria")

        assertEquals(2, result.size)
        with(result[0]) {
            assertEquals("490000053A", id)
            assertEquals("Victoria Station 1", name)
            assertEquals(51.4613, lat, 0.0001)
            assertEquals(-0.1156, lon, 0.0001)
        }
        with(result[1]) {
            assertEquals("490000211S", id)
            assertEquals("Victoria Station 2", name)
        }
    }

    @Test
    fun `returns empty list when response has no matches`() = runTest {
        coEvery { repository.searchStopPoints(any()) } returns StopPointSearchResponse(
            total = 0,
            matches = emptyList()
        )

        val result = useCase("Nowhere")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `passes the query string to the repository unchanged`() = runTest {
        coEvery { repository.searchStopPoints(any()) } returns StopPointSearchResponse()

        useCase("Euston Junction")

        coVerify { repository.searchStopPoints("Euston Junction") }
    }
}
