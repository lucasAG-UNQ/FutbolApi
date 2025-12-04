package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.services.IFootballDataApi
import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import com.grupob.futbolapi.unit.model.builder.TeamBuilder
import com.grupob.futbolapi.unit.model.builder.TeamDTOBuilder
import com.grupob.futbolapi.repositories.TeamRepository
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import com.grupob.futbolapi.services.implementation.TeamService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

    @Mock
    private lateinit var teamRepository: TeamRepository

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService

    @Mock
    private lateinit var playerService: IPlayerService

    @Mock
    private lateinit var footballDataApi: IFootballDataApi

    @InjectMocks
    private lateinit var teamService: TeamService

    @Nested
    @DisplayName("when getTeamWithPlayers(teamName: String) is called")
    inner class GetTeamWithPlayersByName {

        private lateinit var teamName: String
        private lateinit var expectedTeam: Team

        @BeforeEach
        fun setUp() {
            teamName = "Real Madrid"
            val player = PlayerBuilder().withId(1L).withName("Jude Bellingham").build()
            expectedTeam = TeamBuilder().withId(10L).withName(teamName).withPlayer(player).build()
        }

        @Test
        fun shouldReturnTheTeamWhenFoundByTheRepository() {
            // Arrange
            `when`(teamRepository.findByNameWithPlayers(teamName)).thenReturn(expectedTeam)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamName)

            // Assert
            assertEquals(expectedTeam, actualTeam)
            assertEquals(1, actualTeam.players.size)
            assertEquals("Jude Bellingham", actualTeam.players[0].name)

            verify(teamRepository).findByNameWithPlayers(teamName)
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionWhenTheTeamIsNotFoundByTheRepository() {
            // Arrange
            `when`(teamRepository.findByNameWithPlayers(teamName)).thenReturn(null)

            // Act & Assert
            assertThrows<TeamNotFoundException> {
                teamService.getTeamWithPlayers(teamName)
            }

            verify(teamRepository).findByNameWithPlayers(teamName)
        }
    }

    @Nested
    @DisplayName("when getTeamWithPlayers(teamId: Long) is called")
    inner class GetTeamWithPlayersById {

        private var teamId: Long = 10L
        private lateinit var team: Team

        @BeforeEach
        fun setUp() {
            team = TeamBuilder().withId(teamId).withName("Cached Team").withLastUpdated(LocalDateTime.now()).build()
        }

        @Test
        fun shouldReturnTheTeamFromTheRepositoryIfItExistsAndIsRecent() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(team)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamId)

            // Assert
            assertEquals(team, actualTeam)

            // Verify repository was called, but scraper was not
            verify(teamRepository).findByIdWithPlayers(teamId)
            verify(scraperService, never()).getTeam(teamId)
        }

        @Test
        fun shouldCallTheScraperServiceIfTheTeamDoesNotExistInTheRepository() {
            // Arrange
            val scrapedTeamDto = TeamDTOBuilder().withId(teamId).withName("Scraped Team").build()
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(null)
            `when`(scraperService.getTeam(teamId)).thenReturn(scrapedTeamDto)
            `when`(teamRepository.save(org.mockito.ArgumentMatchers.any(Team::class.java))).thenAnswer { it.arguments[0] }


            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamId)

            // Assert
            assertEquals(scrapedTeamDto.id, actualTeam.id)
            assertEquals(scrapedTeamDto.name, actualTeam.name)

            // Verify both repository and scraper were called
            verify(teamRepository).findByIdWithPlayers(teamId)
            verify(scraperService).getTeam(teamId)
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionIfTheScraperFails() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(null)
            `when`(scraperService.getTeam(teamId)).thenThrow(TeamNotFoundException::class.java)

            // Act & Assert
            assertThrows<TeamNotFoundException> {
                teamService.getTeamWithPlayers(teamId)
            }

            // Verify both repository and scraper were called
            verify(teamRepository).findByIdWithPlayers(teamId)
            verify(scraperService).getTeam(teamId)
        }
    }

    @Nested
    @DisplayName("when predictMatch is called")
    inner class PredictMatch {

        private lateinit var teamA: Team
        private lateinit var teamB: Team

        @BeforeEach
        fun setUp() {
            val playerA1 = PlayerBuilder().withRating(8.0).withMinutes(90).build()
            val playerA2 = PlayerBuilder().withRating(9.0).withMinutes(90).build() // Avg: 8.5
            teamA = TeamBuilder().withId(1L).withName("Team A").withPlayers(listOf(playerA1, playerA2)).build()

            val playerB1 = PlayerBuilder().withRating(7.0).withMinutes(90).build()
            val playerB2 = PlayerBuilder().withRating(8.0).withMinutes(90).build() // Avg: 7.5
            teamB = TeamBuilder().withId(2L).withName("Team B").withPlayers(listOf(playerB1, playerB2)).build()
        }

        @Test
        fun shouldPredictTeamAToWinWhenTheirAverageRatingIsHigher() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamB)

            // Act
            val prediction = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamA.id, prediction.predictedWinner?.teamID)
            assertEquals(teamA.name, prediction.predictedWinner?.teamName)
            assertEquals(0.37, prediction.winProbabilityHomeTeam, 0.001)
            assertEquals(0.32, prediction.winProbabilityAwayTeam, 0.001)
            assertEquals(0.31, prediction.drawProbability, 0.001)
        }

        @Test
        fun shouldPredictTeamBToWinWhenTheirAverageRatingIsHigher() {
            // Arrange
            // Swap the teams to make B the winner
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamB)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamA)

            // Act
            val prediction = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamA.id, prediction.predictedWinner?.teamID)
            assertEquals(0.32, prediction.winProbabilityHomeTeam, 0.001)
            assertEquals(0.36, prediction.winProbabilityAwayTeam, 0.001)
            assertEquals(0.32, prediction.drawProbability, 0.001)
        }

        @Test
        fun shouldPredictTeamAHomeTeamToWinInADraw() {
            // Arrange
            val playerB3 = PlayerBuilder().withRating(9.5).withMinutes(90).build() // Adjust rating to make avg 8.5
            teamB.players.add(playerB3)

            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamB)

            // Act
            val prediction = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamA.id, prediction.predictedWinner?.teamID)
            assertEquals(0.34, prediction.winProbabilityHomeTeam, 0.001)
            assertEquals(0.32, prediction.winProbabilityAwayTeam, 0.001)
            assertEquals(0.34, prediction.drawProbability, 0.001)
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionIfTeamAIsNotFound() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(null)
            `when`(scraperService.getTeam(1L)).thenThrow(TeamNotFoundException::class.java)
            // Act & Assert
            assertThrows<TeamNotFoundException> {
                teamService.predictMatch(1L, 2L)
            }
        }

        @Test
        fun shouldThrowTeamNotFoundExceptionIfTeamBIsNotFound() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(null)
            `when`(scraperService.getTeam(2L)).thenThrow(TeamNotFoundException::class.java)


            // Act & Assert
            assertThrows<TeamNotFoundException> {
                teamService.predictMatch(1L, 2L)
            }
        }

        @Test
        fun shouldHandleTeamsWithNoPlayers() {
            // Arrange
            val teamWithNoPlayers = TeamBuilder().withId(3L).withName("No Players FC").withPlayers(emptyList()).build()
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(3L)).thenReturn(teamWithNoPlayers)

            // Act
            val prediction = teamService.predictMatch(1L, 3L)

            // Assert
            assertEquals(teamA.id, prediction.predictedWinner?.teamID, "Team with players should win against team with no players")
            assertEquals(teamA.name, prediction.predictedWinner?.teamName, "Team with players should win against team with no players")
        }

        @Test
        fun shouldHandleTeamsWithPlayersButZeroMinutesPlayed() {
            // Arrange
            val playerA1 = PlayerBuilder().withRating(8.0).withMinutes(0).build()
            val playerA2 = PlayerBuilder().withRating(9.0).withMinutes(0).build()
            val teamAZeroMinutes = TeamBuilder().withId(1L).withName("Team A Zero Minutes").withPlayers(listOf(playerA1, playerA2)).build()

            val playerB1 = PlayerBuilder().withRating(7.0).withMinutes(0).build()
            val playerB2 = PlayerBuilder().withRating(8.0).withMinutes(0).build()
            val teamBZeroMinutes = TeamBuilder().withId(2L).withName("Team B Zero Minutes").withPlayers(listOf(playerB1, playerB2)).build()

            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamAZeroMinutes)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamBZeroMinutes)

            // Act
            val prediction = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamAZeroMinutes.id, prediction.predictedWinner?.teamID)
            assertEquals(0.37, prediction.winProbabilityHomeTeam, 0.001)
            assertEquals(0.32, prediction.winProbabilityAwayTeam, 0.001)
            assertEquals(0.31, prediction.drawProbability, 0.001)
        }

        @Test
        fun shouldHandleTeamsWithAllZeroOrNullPlayerRatings() {
            // Arrange
            val playerA1 = PlayerBuilder().withRating(0.0).withMinutes(90).build()
            val playerA2 = PlayerBuilder().withRating(null).withMinutes(90).build()
            val teamAZeroRatings = TeamBuilder().withId(1L).withName("Team A Zero Ratings").withPlayers(listOf(playerA1, playerA2)).build()

            val playerB1 = PlayerBuilder().withRating(0.0).withMinutes(90).build()
            val playerB2 = PlayerBuilder().withRating(null).withMinutes(90).build()
            val teamBZeroRatings = TeamBuilder().withId(2L).withName("Team B Zero Ratings").withPlayers(listOf(playerB1, playerB2)).build()

            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamAZeroRatings)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamBZeroRatings)

            // Act
            val prediction = teamService.predictMatch(1L, 2L)

            // Assert
            assertNull(prediction.predictedWinner) // Should be a draw
            assertEquals(0.0, prediction.winProbabilityHomeTeam, 0.001)
            assertEquals(0.0, prediction.winProbabilityAwayTeam, 0.001)
            assertEquals(0.34, prediction.drawProbability, 0.001)
        }
    }
}
