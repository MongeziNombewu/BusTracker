package com.tracker.bustracker.domain.usecase

import com.tracker.bustracker.data.remote.dto.DisambiguationOption
import com.tracker.bustracker.data.remote.dto.DisambiguationResult
import com.tracker.bustracker.data.remote.dto.InstructionDto
import com.tracker.bustracker.data.remote.dto.JourneyDto
import com.tracker.bustracker.data.remote.dto.JourneyResultsResponse
import com.tracker.bustracker.data.remote.dto.LegDto
import com.tracker.bustracker.data.remote.dto.LineIdentifierDto
import com.tracker.bustracker.data.remote.dto.ModeDto
import com.tracker.bustracker.data.remote.dto.PlaceDto
import com.tracker.bustracker.data.remote.dto.PointDto
import com.tracker.bustracker.data.remote.dto.RouteOptionDto
import com.tracker.bustracker.data.repository.JourneyRepository
import com.tracker.bustracker.domain.model.JourneyPlanResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlanJourneyUseCaseTest {

    private val repository: JourneyRepository = mockk()
    private lateinit var useCase: PlanJourneyUseCase

    @Before
    fun setUp() {
        useCase = PlanJourneyUseCase(repository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `returns Success with mapped bus legs on happy path`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            journeys = listOf(journey(busLeg()))
        )

        val result = useCase("from", "to")

        assertTrue(result is JourneyPlanResult.Success)
        val legs = (result as JourneyPlanResult.Success).journeys.first().legs
        assertEquals(1, legs.size)
        with(legs.first()) {
            assertEquals("1", lineId)
            assertEquals("One", lineName)
            assertEquals(10, duration)
            assertEquals("Bus towards Victoria Junction", summary)
            assertEquals("Victoria", departureStop)
            assertEquals("Euston Station", arrivalStop)
        }
    }

    @Test
    fun `maps journey duration correctly`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            journeys = listOf(journey(busLeg(), duration = 42))
        )

        val result = (useCase("from", "to") as JourneyPlanResult.Success)
        assertEquals(42, result.journeys.first().duration)
    }

    // ── Leg filtering ─────────────────────────────────────────────────────────

    @Test
    fun `filters out non-bus legs`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            journeys = listOf(
                journey(nonBusLeg("tube"), nonBusLeg("walking"), busLeg(lineId = "1"))
            )
        )

        val result = (useCase("from", "to") as JourneyPlanResult.Success)
        assertEquals(1, result.journeys.first().legs.size)
        assertEquals("1", result.journeys.first().legs.first().lineId)
    }

    @Test
    fun `returns Success with empty legs list when all legs are non-bus`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            journeys = listOf(journey(nonBusLeg("tube"), nonBusLeg("walking")))
        )

        val result = (useCase("from", "to") as JourneyPlanResult.Success)
        assertTrue(result.journeys.first().legs.isEmpty())
    }

    @Test
    fun `returns empty lineId when leg has no routeOptions`() = runTest {
        val leg = LegDto(
            duration = 5,
            instruction = InstructionDto(summary = "Bus"),
            mode = ModeDto(id = "bus"),
            routeOptions = emptyList()
        )
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            journeys = listOf(journey(leg))
        )

        val result = (useCase("from", "to") as JourneyPlanResult.Success)
        assertEquals("", result.journeys.first().legs.first().lineId)
    }

    @Test
    fun `returns Success with empty journeys when response journeys is null`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(journeys = null)

        val result = (useCase("from", "to") as JourneyPlanResult.Success)
        assertTrue(result.journeys.isEmpty())
    }

    @Test
    fun `returns NeedsDisambiguation when from matchStatus is list`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            fromDisambiguation = DisambiguationResult(
                matchStatus = "list",
                disambiguationOptions = listOf(
                    disambiguationOption("1", "Brixton"),
                    disambiguationOption("2", "Brixton Station")
                )
            )
        )

        val result = useCase("from", "to")

        assertTrue(result is JourneyPlanResult.NeedsDisambiguation)
        val disambiguation = result as JourneyPlanResult.NeedsDisambiguation
        assertEquals(2, disambiguation.fromOptions.size)
        assertEquals("Brixton", disambiguation.fromOptions.first().name)
        assertTrue(disambiguation.toOptions.isEmpty())
    }

    @Test
    fun `returns NeedsDisambiguation when to disambiguationOptions has more than one entry`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            toDisambiguation = DisambiguationResult(
                matchStatus = "list",
                disambiguationOptions = listOf(
                    disambiguationOption("A", "London Bridge"),
                    disambiguationOption("B", "London Bridge Station")
                )
            )
        )

        val result = useCase("from", "to")

        assertTrue(result is JourneyPlanResult.NeedsDisambiguation)
        val disambiguation = result as JourneyPlanResult.NeedsDisambiguation
        assertEquals(2, disambiguation.toOptions.size)
        assertTrue(disambiguation.fromOptions.isEmpty())
    }

    @Test
    fun `maps disambiguation stop coordinates correctly`() = runTest {
        coEvery { repository.getJourneyResults(any(), any()) } returns response(
            fromDisambiguation = DisambiguationResult(
                matchStatus = "list",
                disambiguationOptions = listOf(
                    DisambiguationOption(
                        parameterValue = "stop-xyz",
                        place = PlaceDto(commonName = "Oval", lat = 51.48, lon = -0.11)
                    )
                )
            )
        )

        val result = (useCase("from", "to") as JourneyPlanResult.NeedsDisambiguation)
        with(result.fromOptions.first()) {
            assertEquals("stop-xyz", id)
            assertEquals("Oval", name)
            assertEquals(51.48, lat, 0.0001)
            assertEquals(-0.11, lon, 0.0001)
        }
    }

    private fun busLeg(
        lineId: String = "1",
        lineName: String = "One",
        routeOptionName: String = "One",
        duration: Int = 10,
        summary: String = "Bus towards Victoria Junction",
        departureStop: String = "Victoria",
        arrivalStop: String = "Euston Station"
    ) = LegDto(
        duration = duration,
        instruction = InstructionDto(summary = summary),
        mode = ModeDto(id = "bus"),
        routeOptions = listOf(
            RouteOptionDto(
                name = routeOptionName,
                lineIdentifier = LineIdentifierDto(id = lineId, name = lineName)
            )
        ),
        departurePoint = PointDto(commonName = departureStop),
        arrivalPoint = PointDto(commonName = arrivalStop)
    )

    private fun nonBusLeg(modeId: String) = LegDto(
        duration = 5,
        instruction = InstructionDto(summary = "Not a bus leg"),
        mode = ModeDto(id = modeId),
        routeOptions = emptyList()
    )

    private fun journey(vararg legs: LegDto, duration: Int = 20) =
        JourneyDto(duration = duration, legs = legs.toList())

    private fun disambiguationOption(id: String = "stop-1", name: String = "Victoria") =
        DisambiguationOption(
            parameterValue = id,
            place = PlaceDto(commonName = name, lat = 0.0, lon = 1.0)
        )

    private fun response(
        journeys: List<JourneyDto>? = null,
        fromDisambiguation: DisambiguationResult? = null,
        toDisambiguation: DisambiguationResult? = null
    ) = JourneyResultsResponse(
        journeys = journeys,
        fromLocationDisambiguation = fromDisambiguation,
        toLocationDisambiguation = toDisambiguation
    )
}
