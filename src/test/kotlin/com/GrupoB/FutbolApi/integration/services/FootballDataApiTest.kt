package com.grupob.futbolapi.integration.services

import com.grupob.futbolapi.services.implementation.FootballDataApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

class FootballDataApiTest {

    private lateinit var server: MockWebServer
    private lateinit var footballDataApi: FootballDataApi

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        footballDataApi = FootballDataApi()
        ReflectionTestUtils.setField(footballDataApi, "apiKey", "test-api-key")
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getTeam should return a team when a good match is found`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "count": 1,
                    "teams": [
                        {
                            "id": 1,
                            "name": "Boca Juniors",
                            "shortName": "Boca",
                            "tla": "BOC",
                            "crest": "https://crests.football-data.org/1.svg"
                        }
                    ]
                }
            """.trimIndent())
        server.enqueue(mockResponse)

        val team = footballDataApi.getTeam("Boca")

        assertNotNull(team)
        assertEquals("Boca Juniors", team?.getString("name"))
    }

    @Test
    fun `getTeamById should return a team when the team exists`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "id": 1,
                    "name": "Boca Juniors",
                    "shortName": "Boca",
                    "tla": "BOC",
                    "crest": "https://crests.football-data.org/1.svg"
                }
            """.trimIndent())
        server.enqueue(mockResponse)

        val team = footballDataApi.getTeamById(1)

        assertNotNull(team)
        assertEquals("Boca Juniors", team?.getString("name"))
    }
}
