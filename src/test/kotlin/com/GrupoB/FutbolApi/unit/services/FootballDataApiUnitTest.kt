package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.services.implementation.FootballDataApi
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
class FootballDataApiUnitTest {

    @Mock
    private lateinit var client: OkHttpClient

    @Mock
    private lateinit var call: Call

    @InjectMocks
    private lateinit var footballDataApi: FootballDataApi

    @BeforeEach
    fun setUp() {
        `when`(client.newCall(any())).thenReturn(call)
    }

    @Test
    fun `getTeam should return a team when a good match is found`() {
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""
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
            """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull()))
            .build()

        `when`(call.execute()).thenReturn(mockResponse)

        val team = footballDataApi.getTeam("Boca")

        assertNotNull(team)
        assertEquals("Boca Juniors", team?.getString("name"))
    }

    @Test
    fun `getTeamById should return a team when the team exists`() {
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""
                {
                    "id": 1,
                    "name": "Boca Juniors",
                    "shortName": "Boca",
                    "tla": "BOC",
                    "crest": "https://crests.football-data.org/1.svg"
                }
            """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull()))
            .build()

        `when`(call.execute()).thenReturn(mockResponse)

        val team = footballDataApi.getTeamById(1)

        assertNotNull(team)
        assertEquals("Boca Juniors", team?.getString("name"))
    }
}
