package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import com.grupob.futbolapi.unit.model.builder.TeamBuilder
import com.grupob.futbolapi.unit.model.builder.TeamDTOBuilder
import com.grupob.futbolapi.repositories.TeamRepository
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import com.grupob.futbolapi.services.implementation.TeamService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

        @Test
        fun `it should return the team when found by the repository`() {
            // Arrange
            val teamName = "Real Madrid"
            val player = PlayerBuilder().withId(1L).withName("Jude Bellingham").build()
            val expectedTeam = TeamBuilder().withId(10L).withName(teamName).withPlayer(player).build()

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
        fun `it should return null when the team is not found by the repository`() {
            // Arrange
            val teamName = "NonExistent Team"
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

        @Test
        fun `it should return the team from the repository if it exists`() {
            // Arrange
            val teamId = 10L
            val expectedTeam = TeamBuilder().withId(teamId).withName("Cached Team").build()

            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(expectedTeam)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamId)

            // Assert
            assertEquals(expectedTeam, actualTeam)
            assertEquals(teamId, actualTeam?.id)
            assertEquals("Cached Team", actualTeam?.name)

            // Verify repository was called, but scraper was not
            verify(teamRepository).findByIdWithPlayers(teamId)
            verify(scraperService, never()).getTeam(teamId)
        }

        @Test
        fun `it should call the scraper service if the team does not exist in the repository`() {
            // Arrange
            val teamId = 20L
            val scrapedTeamDto = TeamDTOBuilder().withId(teamId).withName("Scraped Team").build()

            // Mock repository to return empty Optional
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(null)
            // Mock scraper to return a team
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
        fun `it should throw an exception if the scraper fails`() {
            // Arrange
            val teamId = 30L
            val scraperException = TeamNotFoundException("Team with id $teamId doesn't seem to exist")

            // Mock repository to return empty Optional
            `when`(teamRepository.findByIdWithPlayers(teamId)).thenReturn(null)
            // Mock scraper to throw an exception
            `when`(scraperService.getTeam(teamId)).thenThrow(scraperException)

            // Act & Assert
            assertNull(teamService.getTeamWithPlayers(teamId))

            // Verify both repository and scraper were called
            verify(teamRepository).findByIdWithPlayers(teamId)
            verify(scraperService).getTeam(teamId)
        }
    }
}