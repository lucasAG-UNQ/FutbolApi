package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.exceptions.PlayerNotFoundException
import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.services.implementation.WhoScoredScraperService
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.format.DateTimeParseException

@DisplayName("WhoScoredScraperService Unit Tests")
class WhoScoredScraperServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var scraperService: WhoScoredScraperService

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString().dropLast(1) // Use a slash for clarity
        scraperService = WhoScoredScraperService(OkHttpClient(), baseUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Nested
    @DisplayName("searchTeams")
    inner class SearchTeams {
        @Test
        fun shouldReturnAListOfTeamsWhenSearchResultsAreFound() {
            // Arrange
            val mockHtml = """
                <html><body>
                    <h2>Teams:</h2>
                    <table>
                        <tr><th>Team</th></tr>
                        <tr><td><a href=\"/teams/13/show/Arsenal-England\">Arsenal</a></td></tr>
                        <tr><td><a href=\"/teams/26/show/Manchester-United-England\">Manchester United</a></td></tr>
                    </table>
                </body></html>
            """
            server.enqueue(MockResponse().setBody(mockHtml))

            // Act
            val result = scraperService.searchTeams("manchester")

            // Assert
            assertEquals(2, result.size)
            assertEquals(13, result[0].teamID)
            assertEquals("Arsenal", result[0].teamName)

            assertEquals(26, result[1].teamID)
            assertEquals("Manchester United", result[1].teamName)

        }

        @Test
        fun shouldReturnAnEmptyListWhenNoTeamsAreFound() {
            // Arrange
            val mockHtml = "<html><body><h2>Teams:</h2><p>No results found.</p></body></html>"
            server.enqueue(MockResponse().setBody(mockHtml))

            // Act
            val result = scraperService.searchTeams("nonexistent")

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun shouldReturnAnEmptyListWhenSearchPageIsNotSuccessful() {
            // Arrange
            server.enqueue(MockResponse().setResponseCode(404))

            // Act
            val result = scraperService.searchTeams("error")

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun shouldReturnAnEmptyListWhenSearchPageHtmlIsEmpty() {
            // Arrange
            server.enqueue(MockResponse().setBody(""))

            // Act
            val result = scraperService.searchTeams("empty")

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getNextTeamMatches")
    inner class GetNextTeamMatches {
        @Test
        fun shouldReturnAListOfUpcomingMatchesWithIndividualParameterAssertions() {
            // Arrange
            val mockHtml = """
                <html><body>
                    <script>
                        require.config.params['args'] = {
                            teamId: 65,
                            fixtureMatches: [
                                [1913918, 1, '16-08-25', '18:30', 51, 'Mallorca', 2, 65, 'Barcelona', 0, '0 : 3', '0 : 2', 1, 1, 'FT', '2025/2026', 'LaLiga', '2', 4, 206, 10803, 24622, 'SLL', 'es', 'es', 0, 1, 0, 'Spain', 'Spain', 'Spain', '0', '3'],
                                [1913888, 1, '23-08-25', '20:30', 832, 'Levante', 0, 65, 'Barcelona', 0, '2 : 3', '2 : 0', 1, 1, 'FT', '2025/2026', 'LaLiga', '2', 4, 206, 10803, 24622, 'SLL', 'es', 'es', 0, 1, 0, 'Spain', 'Spain', 'Spain', '2', '3']
                            ]
                        };
                    </script>
                </body></html>
            """

            server.enqueue(MockResponse().setBody(mockHtml))

            // Act
            val result = scraperService.getNextTeamMatches(10L)

            // Assert
            assertEquals(2, result.size)

            // Assertions for the first match
            assertEquals(1913918, result[0].id)
            assertEquals(51, result[0].homeTeam.teamID)
            assertEquals("Mallorca", result[0].homeTeam.teamName)
            assertEquals(65, result[0].awayTeam.teamID)
            assertEquals("Barcelona", result[0].awayTeam.teamName)
            assertEquals(LocalDate.of(2025, 8, 16), result[0].date)
            assertEquals("LaLiga", result[0].tournament)

            // Assertions for the second match
            assertEquals(1913888, result[1].id)
            assertEquals(832, result[1].homeTeam.teamID)
            assertEquals("Levante", result[1].homeTeam.teamName)
            assertEquals(65, result[1].awayTeam.teamID)
            assertEquals("Barcelona", result[1].awayTeam.teamName)
            assertEquals(LocalDate.of(2025, 8, 23), result[1].date)
            assertEquals("LaLiga", result[1].tournament)
        }

        @Test
        fun shouldReturnAnEmptyListWhenMatchesPageIsNotSuccessful() {
            // Arrange
            server.enqueue(MockResponse().setResponseCode(500))

            // Act
            val result = scraperService.getNextTeamMatches(10L)

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun shouldReturnAnEmptyListWhenMatchesPageHtmlIsEmpty() {
            // Arrange
            server.enqueue(MockResponse().setBody(""))

            // Act
            val result = scraperService.getNextTeamMatches(10L)

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun shouldReturnAnEmptyListWhenNoFixtureMatchesScriptIsFound() {
            // Arrange
            val mockHtml = "<html><body><script>require.config.params['args'] = {teamId: 65,otherData: []};</script></body></html>"
            server.enqueue(MockResponse().setBody(mockHtml))

            // Act
            val result = scraperService.getNextTeamMatches(10L)

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun shouldReturnAnEmptyListWhenFixtureMatchesArrayIsEmpty() {
            // Arrange
            val mockHtml = "<html><body><script>require.config.params['args'] = {teamId: 65,fixtureMatches: []};</script></body></html>"
            server.enqueue(MockResponse().setBody(mockHtml))

            // Act
            val result = scraperService.getNextTeamMatches(10L)

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun shouldThrowJSONExceptionForMalformedFixtureMatchesArray() {
            // Arrange - Malformed JSON array (missing closing bracket, extra comma, etc.)
            val mockHtml = """
                <html><body>
                    <script>
                    require.config.params['args'] = {
                        teamId: 856741856496841,
                        fixtureMatches: [['1', '1', '16-08-25', '18:30', 51, 'Mallorca', 2, 65, 'Barcelona', 0, '0 : 3', '0 : 2', 1, 1, 'FT', '2025/2026', 'LaLiga', '2', 4, 206, 10803, 24622, 'SLL', 'es', 'es', 0, 1, 0, 'Spain', 'Spain', 'Spain', '0', '3'
                        ];
                    };
                    </script>
                </body></html>
            """
            server.enqueue(MockResponse().setBody(mockHtml))

            // Act & Assert
            assertThrows(JSONException::class.java) {
                scraperService.getNextTeamMatches(10L)
            }
        }

        @Test
        fun shouldThrowDateTimeParseExceptionForInvalidDateFormat() {
            // Arrange - Invalid date format
            val mockHtml = """
                <html><body>
                    <script>
                    require.config.params['args'] = {
                        teamId: 856741856496841,
                        fixtureMatches: [['1', '1', 'INVALID-DATE', '18:30', 51, 'Mallorca', 2, 65, 'Barcelona', 0, '0 : 3', '0 : 2', 1, 1, 'FT', '2025/2026', 'LaLiga', '2', 4, 206, 10803, 24622, 'SLL', 'es', 'es', 0, 1, 0, 'Spain', 'Spain', 'Spain', '0', '3']];
                    }
                    </script>
                </body></html>
            """
            server.enqueue(MockResponse().setBody(mockHtml))

            // Act & Assert
            assertThrows(DateTimeParseException::class.java) {
                scraperService.getNextTeamMatches(10L)
            }
        }
    }

    @Nested
    @DisplayName("getTeam")
    inner class GetTeam {
        @Test
        fun shouldReturnATeamWithPlayers() {
            // Arrange
            val mockJson = """
                {
                    "playerTableStats": [
                        {
                            "playerId": 1, "name": "Player One", "teamName": "Test FC", "teamRegionName": "Testland", "teamId": 100,
                            "positionText": "Defender", "tournamentName": "Test League", "seasonName": "2024", "apps": 10, "goal": 1, "assistTotal": 2, 
                            "rating": 7.5, "minsPlayed": 900, "yellowCard": 2, "redCard": 0, "age": 25
                        },
                        {
                            "playerId": 2, "name": "Player Two", "teamName": "Test FC", "teamRegionName": "Testland", "teamId": 100,
                            "positionText": "Forward", "tournamentName": "Test League", "seasonName": "2024", "apps": 12, "goal": 5, "assistTotal": 1, 
                            "rating": 8.1, "minsPlayed": 1000, "yellowCard": 1, "redCard": 0, "age": 28
                        }
                    ]
                }
            """
            server.enqueue(MockResponse().setBody(mockJson))

            // Act
            val result = scraperService.getTeam(100L)

            // Assert
            assertEquals(100L, result.id)
            assertEquals("Test FC", result.name)
            assertEquals("Testland", result.country)
            assertEquals(2, result.players.size)
            assertEquals("Player One", result.players[0].name)
            assertEquals(28, result.players[1].age)
        }

        @Test
        fun shouldReturnATeamWithPlayersEvenIfSomePlayerFieldsAreMissing() {
            // Arrange
            val mockJson = """
                {
                    "playerTableStats": [
                        {
                            "playerId": 1, "name": "Player One", "teamName": "Test FC", "teamRegionName": "Testland", "teamId": 100
                        }
                    ]
                }
            """
            server.enqueue(MockResponse().setBody(mockJson))

            // Act
            val result = scraperService.getTeam(100L)

            // Assert
            assertEquals(100L, result.id)
            assertEquals("Test FC", result.name)
            assertEquals("Testland", result.country)
            assertEquals(1, result.players.size)
            assertEquals("Player One", result.players[0].name)
            assertNull(result.players[0].position)
            assertNull(result.players[0].tournament)
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionIfPlayerTableStatsIsEmpty() {
            // Arrange: Prepare the server for BOTH requests the service will make.

            // 1. First response: An empty player stats array for the initial API call.
            server.enqueue(MockResponse().setBody("{\"playerTableStats\": []}"))

            // 2. Second response: An HTML page that does NOT contain the team name, for the fallback scrape.
            server.enqueue(MockResponse().setBody("<html><body>Invalid Page</body></html>"))

            // Act & Assert - Using AssertJ for a more fluent assertion
            assertThatThrownBy { scraperService.getTeam(100L) }
                .isInstanceOf(TeamNotFoundException::class.java)
                .hasMessageContaining("Team with id 100 doesn't seems to exist")
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionIfPlayerTableStatsIsMissing() {
            // Arrange
            val mockJson = "{\"otherData\": \"value\"}"
            server.enqueue(MockResponse().setBody(mockJson))

            // Act & Assert
            assertThrows(TeamNotFoundException::class.java) {
                scraperService.getTeam(100L)
            }
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionForUnsuccessfulHttpResponseForGetTeam() {
            // Arrange
            server.enqueue(MockResponse().setResponseCode(404))

            // Act & Assert
            assertThrows(TeamNotFoundException::class.java) {
                scraperService.getTeam(100L)
            }
        }
    }


    @Nested
    @DisplayName("getPlayerById")
    inner class GetPlayer{

        @Test
        fun getPlayerByIdShouldReturnAPlayerWhenTheApiCallIsSuccessful() {
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
        fun getPlayerByIdShouldThrowPlayerNotFoundExceptionWhenPlayerTableStatsIsEmpty() {
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .setBody("""{ "playerTableStats": [] }""")
            server.enqueue(mockResponse)

            assertThrows<PlayerNotFoundException> {
                scraperService.getPlayerById(1L)
            }
        }

        @Test
        fun getPlayerByIdShouldThrowPlayerNotFoundExceptionOnApiError() {
            val mockResponse = MockResponse().setResponseCode(500)
            server.enqueue(mockResponse)

            assertThrows<PlayerNotFoundException> {
                scraperService.getPlayerById(1L)
            }
        }

        @Test
        fun getPlayerByIdShouldHandleMissingOptionalFieldsGracefully() {
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
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
            """.trimIndent()
                )
            server.enqueue(mockResponse)

            val player = scraperService.getPlayerById(1L)

            assertEquals("Lamine Yamal", player.name)
            assertEquals(18, player.age)
            assertEquals(0, player.apps) // Default value
            assertEquals(0, player.goals) // Default value
            assertEquals(8.25, player.rating)
        }

        @Test
        fun getPlayerByIdShouldAggregateStatsFromMultipleTournaments() {
            val mockResponse = MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
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
                        },
                        {
                            "name": "Lamine Yamal",
                            "positionText": "Forward",
                            "age": 18,
                            "apps": 4,
                            "minsPlayed": 335,
                            "goal": 2,
                            "assistTotal": 1,
                            "yellowCard": 2,
                            "redCard": 0,
                            "rating": 7.82,
                            "tournamentName": "Champions League",
                            "seasonName": "2025/2026"
                        }
                    ]
                }
            """.trimIndent()
                )
            server.enqueue(mockResponse)

            val player = scraperService.getPlayerById(1L)

            assertEquals("Lamine Yamal", player.name)
            assertEquals(18, player.age)
            assertEquals(14, player.apps)
            assertEquals(7, player.goals)
            assertEquals(8, player.assists)
            assertEquals(2, player.yellowCards)
            assertEquals(0, player.redCards)
            assertEquals(8.12, player.rating!!, 0.01)
            assertEquals("LaLiga, Champions League", player.tournament)
        }
    }
}