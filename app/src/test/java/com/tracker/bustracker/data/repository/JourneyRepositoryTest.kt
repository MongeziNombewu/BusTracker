package com.tracker.bustracker.data.repository

import com.tracker.bustracker.data.remote.TflApiService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class JourneyRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: JourneyRepository
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TflApiService::class.java)

        repository = JourneyRepository(api, json)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getJourneyResults returns parsed response on 200`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(JOURNEY_SUCCESS_JSON)
        )

        val result = repository.getJourneyResults("490000053A", "490000211S")

        assertNotNull(result.journeys)
        assertEquals(1, result.journeys!!.size)
        with(result.journeys.first()) {
            assertEquals(22, duration)
            assertEquals(2, legs.size)
        }
    }

    @Test
    fun `getJourneyResults maps bus leg fields correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(JOURNEY_SUCCESS_JSON)
        )

        val result = repository.getJourneyResults("from", "to")
        val busLeg = result.journeys!!.first().legs.first { it.mode.id == "bus" }

        assertEquals("35", busLeg.routeOptions.first().lineIdentifier?.id)
        assertEquals("Brixton", busLeg.departurePoint?.commonName)
        assertEquals("Clapham Junction", busLeg.arrivalPoint?.commonName)
        assertEquals(12, busLeg.duration)
    }

    @Test
    fun `getJourneyResults returns null journeys when field is absent`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"fromLocationDisambiguation": null, "toLocationDisambiguation": null}""")
        )

        val result = repository.getJourneyResults("from", "to")

        assertNull(result.journeys)
    }

    @Test
    fun `getJourneyResults parses disambiguation from 300 error body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(300)
                .setBody(DISAMBIGUATION_JSON)
        )

        val result = repository.getJourneyResults("Brixton", "London Bridge")

        val fromDisambiguation = result.fromLocationDisambiguation
        assertNotNull(fromDisambiguation)
        assertEquals("list", fromDisambiguation!!.matchStatus)
        assertEquals(2, fromDisambiguation.disambiguationOptions.size)
        assertEquals("Brixton", fromDisambiguation.disambiguationOptions.first().place.commonName)
    }

    @Test
    fun `getJourneyResults maps disambiguation option parameterValue correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(300)
                .setBody(DISAMBIGUATION_JSON)
        )

        val result = repository.getJourneyResults("Brixton", "to")
        val option = result.fromLocationDisambiguation!!.disambiguationOptions.first()

        assertEquals("490000053A", option.parameterValue)
    }

    @Test(expected = IllegalStateException::class)
    fun `getJourneyResults throws IllegalStateException on 500`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        repository.getJourneyResults("from", "to")
    }

    @Test
    fun `getJourneyResults exception message includes status code`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))

        val error = runCatching { repository.getJourneyResults("from", "to") }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("503"))
    }

    @Test
    fun `searchStopPoints returns parsed list of matches`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(STOP_SEARCH_JSON)
        )

        val result = repository.searchStopPoints("Brixton")

        assertEquals(2, result.matches.size)
        with(result.matches.first()) {
            assertEquals("490000053A", id)
            assertEquals("Brixton Station", name)
            assertEquals(51.4613, lat, 0.0001)
            assertEquals(-0.1156, lon, 0.0001)
        }
    }

    @Test
    fun `searchStopPoints returns empty matches when total is zero`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"total": 0, "matches": []}""")
        )

        val result = repository.searchStopPoints("Nowhere")

        assertTrue(result.matches.isEmpty())
    }

    // These JSON payloads were generated using AI. Alternatively I could have used the actual payload and read it from file
    companion object {

        private val JOURNEY_SUCCESS_JSON = """
            {
              "journeys": [
                {
                  "duration": 22,
                  "legs": [
                    {
                      "duration": 12,
                      "instruction": { "summary": "Bus to Clapham Junction", "detailed": "" },
                      "mode": { "id": "bus", "name": "Bus" },
                      "routeOptions": [
                        {
                          "name": "35",
                          "lineIdentifier": { "id": "35", "name": "35" }
                        }
                      ],
                      "departurePoint": { "commonName": "Brixton", "lat": 51.4613, "lon": -0.1156 },
                      "arrivalPoint": { "commonName": "Clapham Junction", "lat": 51.4641, "lon": -0.1685 }
                    },
                    {
                      "duration": 10,
                      "instruction": { "summary": "Walk to destination", "detailed": "" },
                      "mode": { "id": "walking", "name": "Walking" },
                      "routeOptions": [],
                      "departurePoint": { "commonName": "Clapham Junction", "lat": 51.4641, "lon": -0.1685 },
                      "arrivalPoint": { "commonName": "Destination", "lat": 51.4650, "lon": -0.1700 }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        private val DISAMBIGUATION_JSON = """
            {
              "fromLocationDisambiguation": {
                "matchStatus": "list",
                "disambiguationOptions": [
                  {
                    "parameterValue": "490000053A",
                    "place": {
                      "commonName": "Brixton",
                      "placeType": "StopPoint",
                      "lat": 51.4613,
                      "lon": -0.1156
                    }
                  },
                  {
                    "parameterValue": "490000053B",
                    "place": {
                      "commonName": "Brixton Station",
                      "placeType": "StopPoint",
                      "lat": 51.4620,
                      "lon": -0.1150
                    }
                  }
                ]
              },
              "toLocationDisambiguation": {
                "matchStatus": "",
                "disambiguationOptions": []
              }
            }
        """.trimIndent()

        private val STOP_SEARCH_JSON = """
            {
              "total": 2,
              "matches": [
                { "id": "490000053A", "name": "Brixton Station", "lat": 51.4613, "lon": -0.1156 },
                { "id": "490000053B", "name": "Brixton Road",    "lat": 51.4620, "lon": -0.1150 }
              ]
            }
        """.trimIndent()
    }
}
