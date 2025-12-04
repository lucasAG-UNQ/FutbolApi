package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.exceptions.PlayerNotFoundException
import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.services.IWhoScoredScraperService
import com.grupob.futbolapi.services.implementation.PlayerService
import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class PlayerServiceTest {

    @Mock
    private lateinit var playerRepository: PlayerRepository

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService

    @InjectMocks
    private lateinit var playerService: PlayerService

    private val playerId = 1L
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        player = PlayerBuilder().withId(playerId).withName("Test Player").build()
    }

    @Test
    fun findByPlayerIdShouldReturnPlayerFromRepositoryIfItExists() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))

        val result = playerService.findByPlayerId(playerId)

        assertEquals(player, result)
        verify(scraperService, never()).getPlayerById(playerId)
    }

    @Test
    fun findByPlayerIdShouldScrapeAndSavePlayerIfNotInRepository() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.empty())
        `when`(scraperService.getPlayerById(playerId)).thenReturn(player)
        `when`(playerRepository.save(player)).thenReturn(player)

        val result = playerService.findByPlayerId(playerId)

        assertEquals(player, result)
        verify(playerRepository).save(player)
    }

    @Test
    fun findByPlayerIdShouldReturnNullIfPlayerIsNotInRepositoryAndScraperFails() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.empty())
        `when`(scraperService.getPlayerById(playerId)).thenThrow(PlayerNotFoundException("Player not found"))

        val result = playerService.findByPlayerId(playerId)

        assertNull(result)
    }
}
