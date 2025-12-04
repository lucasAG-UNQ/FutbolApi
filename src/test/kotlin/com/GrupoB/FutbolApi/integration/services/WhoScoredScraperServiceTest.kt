package com.grupob.futbolapi.integration.services

import com.grupob.futbolapi.exceptions.PlayerNotFoundException
import com.grupob.futbolapi.services.implementation.WhoScoredScraperService
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


@Tag("integration")
class WhoScoredScraperServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var scraperService: WhoScoredScraperService

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        scraperService = WhoScoredScraperService(OkHttpClient(), server.url("/").toString())
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getPlayerById should return a player when the API call is successful`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "playerTableStats": [
                        {
                            "name": "Lamine Yamal",
                            "positionText": "Forward",
                            "age": 18,
                            "apps": 10,
                            "minsPlayed": 814,
                            "goal": 5,
                            "assistTotal": 7,
                            "yellowCard": 0,
                            "redCard": 0,
                            "rating": 8.25,
                            "tournamentName": "LaLiga",
                            "seasonName": "2025/2026"
                        }
                    ]
                }
            """.trimIndent())
        server.enqueue(mockResponse)

        val player = scraperService.getPlayerById(1L)

        assertEquals("Lamine Yamal", player.name)
        assertEquals(18, player.age)
        assertEquals(10, player.apps)
        assertEquals(5, player.goals)
        assertEquals(8.25, player.rating)
    }

    @Test
    fun `getPlayerById should throw PlayerNotFoundException when playerTableStats is empty`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""{ "playerTableStats": [] }""")
        server.enqueue(mockResponse)

        assertThrows<PlayerNotFoundException> {
            scraperService.getPlayerById(1L)
        }
    }

    @Test
    fun `getPlayerById should throw PlayerNotFoundException on API error`() {
        val mockResponse = MockResponse().setResponseCode(500)
        server.enqueue(mockResponse)

        assertThrows<PlayerNotFoundException> {
            scraperService.getPlayerById(1L)
        }
    }

    @Test
    fun `getPlayerById should handle missing optional fields gracefully`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "playerTableStats": [
                        {
                            "name": "Lamine Yamal",
                            "age": 18,
                            "minsPlayed": 814,
                            "rating": 8.25,
                            "tournamentName": "LaLiga",
                            "seasonName": "2025/2026"
                        }
                    ]
                }
            """.trimIndent())
        server.enqueue(mockResponse)

        val player = scraperService.getPlayerById(1L)

        assertEquals("Lamine Yamal", player.name)
        assertEquals(18, player.age)
        assertEquals(0, player.apps) // Default value
        assertEquals(0, player.goals) // Default value
        assertEquals(8.25, player.rating)
    }
}
