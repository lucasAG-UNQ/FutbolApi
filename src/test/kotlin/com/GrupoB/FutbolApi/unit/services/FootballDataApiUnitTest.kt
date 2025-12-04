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
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class FootballDataApiUnitTest {

    @Mock
    private lateinit var client: OkHttpClient

    @Mock
    private lateinit var call: Call

    private lateinit var footballDataApi: FootballDataApi

    @BeforeEach
    fun setUp() {
        footballDataApi = FootballDataApi(client)
        `when`(client.newCall(any())).thenReturn(call)
        ReflectionTestUtils.setField(footballDataApi, "apiKey", "test-api-key")
    }

    @Test
    fun getTeamShouldReturnATeamWhenAGoodMatchIsFound() {
        val firstPageResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""
                {
                    "count": 1,
                    "filters": {
                        "searchQuery": null,
                        "permission": "TIER_ONE"
                    },
                    "teams": [
                        {
                            "id": 2736,
                            "name": "Cronulla Seagulls FC",
                            "shortName": "Cronulla",
                            "tla": "CRO",
                            "crest": null,
                            "address": "null Sutherland null",
                            "website": "http://www.cronullaseagulls.com/",
                            "founded": 1959,
                            "clubColors": "Green / White",
                            "venue": null,
                            "lastUpdated": "2019-04-04T03:22:19Z"
                        }
                    ]
                }
            """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull()))
            .build()

        val secondPageResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""
                {
                    "count": 0,
                    "filters": {},
                    "teams": []
                }
            """.trimIndent().toResponseBody("application/json".toMediaTypeOrNull()))
            .build()

        `when`(call.execute()).thenReturn(firstPageResponse, secondPageResponse)

        val team = footballDataApi.getTeam("Cronulla Seagul")

        assertNotNull(team)
        assertEquals("Cronulla Seagulls FC", team?.getString("name"))
    }

    @Test
    fun getTeamByIdShouldReturnATeamWhenTheTeamExists() {
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
