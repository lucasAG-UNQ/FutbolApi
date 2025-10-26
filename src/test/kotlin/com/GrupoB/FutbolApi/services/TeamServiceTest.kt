package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.builder.PlayerBuilder
import com.grupob.futbolapi.model.builder.TeamBuilder
import com.grupob.futbolapi.repositories.TeamRepository
import com.grupob.futbolapi.services.implementation.TeamService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
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
    @DisplayName("when getTeamWithPlayers is called")
    inner class GetTeamWithPlayers {

        @Test
        fun `it should return the team when found by the repository`() {
            // Arrange
            val teamName = "Real Madrid"
            val player = PlayerBuilder().withId(1L).withName("Jude Bellingham").build()
            val expectedTeam = TeamBuilder().withId(10L).withName(teamName).withPlayer(player).build()

            // Mock the repository call
            `when`(teamRepository.findByNameWithPlayers(teamName)).thenReturn(expectedTeam)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamName)

            // Assert
            assertEquals(expectedTeam.id, actualTeam?.id, "The returned team should match the one from the repository")
            assertEquals(1, actualTeam?.players?.size)
            assertEquals("Jude Bellingham", actualTeam?.players?.get(0)?.name)

            // Verify that the repository method was called exactly once
            verify(teamRepository).findByNameWithPlayers(teamName)
        }

        @Test
        fun `it should return null when the team is not found by the repository`() {
            // Arrange
            val teamName = "NonExistent Team"

            // Mock the repository call to return null
            `when`(teamRepository.findByNameWithPlayers(teamName)).thenReturn(null)

            // Act
            val actualTeam = teamService.getTeamWithPlayers(teamName)

            // Assert
            assertNull(actualTeam, "The service should return null when the repository finds nothing")

            // Verify that the repository method was called exactly once
            verify(teamRepository).findByNameWithPlayers(teamName)
        }
    }
}