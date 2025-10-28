package com.grupob.futbolapi.unit.services

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

@ExtendWith(MockitoExtension::class)
@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

    @Mock
    private lateinit var teamRepository: TeamRepository

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService

    @Mock
    private lateinit var playerService: IPlayerService

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
        fun itShouldReturnTheTeamWhenFoundByTheRepository() {
            // Arrange
            `when`(teamRepository.findByNameWithPlayers(teamName)).thenReturn(expectedTeam)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamName)

            // Assert
            assertEquals(expectedTeam, actualTeam)
            assertEquals(1, actualTeam?.players?.size)
            assertEquals("Jude Bellingham", actualTeam?.players?.get(0)?.name)

            verify(teamRepository).findByNameWithPlayers(teamName)
        }

        @Test
        fun itShouldReturnNullWhenTheTeamIsNotFoundByTheRepository() {
            // Arrange
            `when`(teamRepository.findByNameWithPlayers(teamName)).thenReturn(null)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamName)

            // Assert
            assertNull(actualTeam)

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
            team = TeamBuilder().withId(teamId).withName("Cached Team").build()
        }

        @Test
        fun itShouldReturnTheTeamFromTheRepositoryIfItExists() {
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
        fun itShouldCallTheScraperServiceIfTheTeamDoesNotExistInTheRepository() {
            // Arrange
            val scrapedTeamDto = TeamDTOBuilder().withId(teamId).withName("Scraped Team").build()
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(null)
            `when`(scraperService.getTeam(teamId)).thenReturn(scrapedTeamDto)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamId)

            // Assert
            assertEquals(scrapedTeamDto.id, actualTeam?.id)
            assertEquals(scrapedTeamDto.name, actualTeam?.name)

            // Verify both repository and scraper were called
            verify(teamRepository).findByIdWithPlayers(teamId)
            verify(scraperService).getTeam(teamId)
        }

        @Test
        fun itShouldReturnNullIfTheScraperFails() {
            // Arrange
            val scraperException = TeamNotFoundException("Team with id $teamId doesn't seem to exist")
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(null)
            `when`(scraperService.getTeam(teamId)).thenThrow(scraperException)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamId)

            // Assert
            assertNull(actualTeam)

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
            val playerA1 = PlayerBuilder().withRating(8.0).build()
            val playerA2 = PlayerBuilder().withRating(9.0).build() // Avg: 8.5
            teamA = TeamBuilder().withId(1L).withName("Team A").withPlayers(listOf(playerA1, playerA2)).build()

            val playerB1 = PlayerBuilder().withRating(7.0).build()
            val playerB2 = PlayerBuilder().withRating(8.0).build() // Avg: 7.5
            teamB = TeamBuilder().withId(2L).withName("Team B").withPlayers(listOf(playerB1, playerB2)).build()
        }

        @Test
        fun itShouldPredictTeamAToWinWhenTheirAverageRatingIsHigher() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamB)

            // Act
            val winner = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamA, winner)
        }

        @Test
        fun itShouldPredictTeamBToWinWhenTheirAverageRatingIsHigher() {
            // Arrange
            // Swap the teams to make B the winner
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamB)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamA)

            // Act
            val winner = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamA, winner) // The service returns the second team if their rating is higher
        }

        @Test
        fun itShouldPredictTeamAHomeTeamToWinInADraw() {
            // Arrange
            val playerB3 = PlayerBuilder().withRating(10.0).build() // Make B's avg 8.5 too
            teamB.players.add(playerB3)

            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(teamB)

            // Act
            val winner = teamService.predictMatch(1L, 2L)

            // Assert
            assertEquals(teamA, winner)
        }

        @Test
        fun itShouldThrowTeamNotFoundExceptionIfTeamAIsNotFound() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(null)
            `when`(scraperService.getTeam(1L)).thenThrow(TeamNotFoundException::class.java)
            // Act & Assert
            assertThrows<TeamNotFoundException> {
                teamService.predictMatch(1L, 2L)
            }
        }

        @Test
        fun itShouldThrowTeamNotFoundExceptionIfTeamBIsNotFound() {
            // Arrange
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(2L)).thenReturn(null)

            // Act & Assert
            assertThrows<TeamNotFoundException> {
                teamService.predictMatch(1L, 2L)
            }
        }

        @Test
        fun itShouldHandleTeamsWithNoPlayers() {
            // Arrange
            val teamWithNoPlayers = TeamBuilder().withId(3L).withName("No Players FC").withPlayers(emptyList()).build()
            `when`(teamRepository.findByIdWithPlayers(1L)).thenReturn(teamA)
            `when`(teamRepository.findByIdWithPlayers(3L)).thenReturn(teamWithNoPlayers)

            // Act
            val winner = teamService.predictMatch(1L, 3L)

            // Assert
            assertEquals(teamA.id, winner?.id, "Team with players should win against team with no players")
            assertEquals(teamA.name, winner?.name, "Team with players should win against team with no players")
        }
    }
}