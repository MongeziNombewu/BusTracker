package com.tracker.bustracker.presentation.journeyresults

import com.tracker.bustracker.domain.model.BusLeg
import com.tracker.bustracker.domain.model.JourneyOption
import com.tracker.bustracker.domain.model.JourneyPlanResult
import com.tracker.bustracker.domain.model.StopPoint
import com.tracker.bustracker.domain.usecase.PlanJourneyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyResultsViewModelTest {
    private val planJourney: PlanJourneyUseCase = mockk()
    private lateinit var viewModel: JourneyResultsViewModel
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = JourneyResultsViewModel(planJourney)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertEquals(JourneyResultsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loadJourney emits Results when planJourney returns journeys with bus legs`() = runTest {
        val journey = journeyOption(busLeg(), busLeg())
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(listOf(journey))

        viewModel.loadJourney("from-id", "to-id")

        val state = viewModel.uiState.value
        assertTrue(state is JourneyResultsUiState.Results)
        val results = state as JourneyResultsUiState.Results
        assertEquals(1, results.journeys.size)
        assertEquals(2, results.journeys.first().legs.size)
    }

    @Test
    fun `loadJourney passes from and to ids to use case`() = runTest {
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(
            listOf(journeyOption(busLeg()))
        )

        viewModel.loadJourney("490000053A", "490000211S")

        coVerify { planJourney("490000053A", "490000211S") }
    }

    @Test
    fun `loadJourney emits NoBusRoutes when all journeys have empty legs`() = runTest {
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(
            listOf(journeyOption())
        )

        viewModel.loadJourney("from", "to")

        assertEquals(JourneyResultsUiState.NoBusRoutes, viewModel.uiState.value)
    }

    @Test
    fun `loadJourney emits NoBusRoutes when success returns empty journey list`() = runTest {
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(emptyList())

        viewModel.loadJourney("from", "to")

        assertEquals(JourneyResultsUiState.NoBusRoutes, viewModel.uiState.value)
    }

    @Test
    fun `loadJourney emits JourneyDisambiguation when planJourney needs disambiguation`() = runTest {
        val fromOptions = listOf(stopPoint(), stopPoint())
        val toOptions = listOf(stopPoint())
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.NeedsDisambiguation(
            fromOptions = fromOptions,
            toOptions = toOptions
        )

        viewModel.loadJourney("Brixton", "London Bridge")

        val state = viewModel.uiState.value
        assertTrue(state is JourneyResultsUiState.JourneyDisambiguation)
        val disambiguation = state as JourneyResultsUiState.JourneyDisambiguation
        assertEquals(2, disambiguation.fromOptions.size)
        assertEquals(1, disambiguation.toOptions.size)
    }

    @Test
    fun `loadJourney emits Error when planJourney throws`() = runTest {
        coEvery { planJourney(any(), any()) } throws RuntimeException("No internet")

        viewModel.loadJourney("from", "to")

        val state = viewModel.uiState.value
        assertTrue(state is JourneyResultsUiState.Error)
        assertEquals("No internet", (state as JourneyResultsUiState.Error).message)
    }

    @Test
    fun `onDisambiguationResolved with from stop updates from id and refetches`() = runTest {
        val resolvedFrom = stopPoint(id = "resolved-from")
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(
            listOf(journeyOption(busLeg()))
        )

        viewModel.loadJourney("original-from", "original-to")
        viewModel.onDisambiguationResolved(from = resolvedFrom, to = null)

        // Should have refetched with the resolved from id, keeping original to
        coVerify { planJourney("resolved-from", "original-to") }
    }

    @Test
    fun `onDisambiguationResolved with to stop updates to id and refetches`() = runTest {
        val resolvedTo = stopPoint("resolved-to")
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(
            listOf(journeyOption(busLeg()))
        )

        viewModel.loadJourney("original-from", "original-to")
        viewModel.onDisambiguationResolved(from = null, to = resolvedTo)

        coVerify { planJourney("original-from", "resolved-to") }
    }

    @Test
    fun `onDisambiguationResolved with null from keeps previous from id`() = runTest {
        coEvery { planJourney(any(), any()) } returns JourneyPlanResult.Success(
            listOf(journeyOption(busLeg()))
        )

        viewModel.loadJourney("kept-from", "original-to")
        viewModel.onDisambiguationResolved(from = null, to = stopPoint("new-to"))

        coVerify { planJourney("kept-from", "new-to") }
    }

    private fun busLeg(lineId: String = "123") = BusLeg(
        lineId = lineId,
        lineName = lineId,
        duration = 10,
        summary = "Bus towards XXX Stop",
        departureStop = "Previous STop",
        arrivalStop = "Next Stop"
    )

    private fun journeyOption(vararg legs: BusLeg, duration: Int = 20) =
        JourneyOption(duration = duration, legs = legs.toList())

    private fun stopPoint(id: String = "stop-1", name: String = "Next Stop") =
        StopPoint(id = id, name = name, lat = 0.0, lon = 0.0)
}
