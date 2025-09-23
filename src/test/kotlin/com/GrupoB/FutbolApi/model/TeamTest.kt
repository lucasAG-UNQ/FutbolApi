package com.grupob.futbolapi.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TeamTest {

    @Test
    fun `Team can be created with valid data`() {
        val team = Team(id = 1L, name = "Test Team")
        assertEquals(1L, team.id)
        assertEquals("Test Team", team.name)
        assertEquals(0, team.players.size)
    }

    @Test
    fun `Adding a player to a team works correctly`() {
        val team = Team(id = 1L, name = "Test Team")
        val player = Player(id = 1L, name = "Test Player", position = "Forward", team = team)
        team.players.add(player)

        assertEquals(1, team.players.size)
        assertEquals(player, team.players[0])
    }
}