package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.services.IWhoScoredScraperService
import com.grupob.futbolapi.services.implementation.PlayerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("PlayerService Unit Tests")
class PlayerServiceTest {

    @Mock
    private lateinit var playerRepository: PlayerRepository

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService

    private lateinit var playersToSave: List<Player>

    @InjectMocks
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setUp() {
        val player1 = PlayerBuilder().withId(1L).withName("Player One").build()
        val player2 = PlayerBuilder().withId(2L).withName("Player Two").build()
        playersToSave = listOf(player1, player2)
    }

    @Test
    fun saveAllShouldCallTheRepositorysSaveAllMethodWithTheCorrectListOfPlayers() {
        // Act
        playerService.saveAll(playersToSave)

        // Assert
        // Verify that the repository's saveAll method was called exactly once
        // with the same list of players that was passed to the service.
        verify(playerRepository).saveAll(playersToSave)
    }

    @Test
    fun saveAllShouldHandleAnEmptyListWithoutErrors() {
        // Arrange
        val emptyPlayerList = emptyList<Player>()

        // Act
        playerService.saveAll(emptyPlayerList)

        // Assert
        // Verify that the repository's saveAll method was still called with the empty list.
        verify(playerRepository).saveAll(emptyPlayerList)
    }
}