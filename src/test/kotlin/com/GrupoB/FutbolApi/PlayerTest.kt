package com.grupob.futbolapi.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlayerTest {

    @Test
    fun `test player creation and properties`() {
        // Given
        val playerName = "Lionel Messi"
        val playerPosition = "Forward"

        // When
        val player = Player(
            id = 1L,
            name = playerName,
            position = playerPosition
        )

        // Then
        assertEquals(1L, player.id)
        assertEquals(playerName, player.name)
        assertEquals(playerPosition, player.position)
        assertNull(player.team, "Team should be null initially")
    }

    @Test
    fun `test player association with a team`() {
        // Given
        val team = Team(id = 1L, name = "FC Barcelona")
        val player = Player(
            id = 10L,
            name = "Lionel Messi",
            position = "Forward"
        )

        // When
        player.team = team

        // Then
        assertNotNull(player.team)
        assertEquals(team, player.team)
        assertEquals(team.id, player.team?.id)
        assertEquals(team.name, player.team?.name)
    }

    @Test
    fun `test updating player properties`() {
        // Given
        val player = Player(
            id = 7L,
            name = "Old Name",
            position = "Old Position"
        )

        // When
        player.name = "Cristiano Ronaldo"
        player.position = "Striker"

        // Then
        assertEquals("Cristiano Ronaldo", player.name)
        assertEquals("Striker", player.position)
    }
}
