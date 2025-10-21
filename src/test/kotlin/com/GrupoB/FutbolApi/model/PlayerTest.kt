package com.grupob.futbolapi.model

import org.junit.jupiter.api.Assertions
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
        Assertions.assertEquals(1L, player.id)
        Assertions.assertEquals(playerName, player.name)
        Assertions.assertEquals(playerPosition, player.position)
        Assertions.assertNull(player.team, "Team should be null initially")
    }

    @Test
    fun `test player association with a team`() {
        // Given
        val team = Team(whoscoredId = 1L, name = "FC Barcelona")
        val player = Player(
            id = 10L,
            name = "Lionel Messi",
            position = "Forward"
        )

        // When
        player.team = team

        // Then
        Assertions.assertNotNull(player.team)
        Assertions.assertEquals(team, player.team)
        Assertions.assertEquals(team.whoscoredId, player.team?.whoscoredId)
        Assertions.assertEquals(team.name, player.team?.name)
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
        Assertions.assertEquals("Cristiano Ronaldo", player.name)
        Assertions.assertEquals("Striker", player.position)
    }
}