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
}