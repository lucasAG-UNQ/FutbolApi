package com.grupob.futbolapi.model

import com.grupob.futbolapi.model.builder.PlayerBuilder
import com.grupob.futbolapi.model.builder.TeamBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Team Model Tests")
class TeamTest {

    @Test
    fun `a Team can be created with valid data`() {
        val team = TeamBuilder()
            .withId(1L)
            .withName("Test Team")
            .build()

        assertEquals(1L, team.id)
        assertEquals("Test Team", team.name)
        assertTrue(team.players.isEmpty(), "A new team should have no players initially")
    }

    @Test
    fun `a Team can be created with a null id`() {
        val newTeam = TeamBuilder()
            .withId(null)
            .withName("New Team")
            .build()

        assertNull(newTeam.id, "A new team should have a null ID before being persisted")
        assertEquals("New Team", newTeam.name)
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    inner class EqualityTests {

        @Test
        fun `two Team instances with different ids are not equal`() {
            val team1 = TeamBuilder().withId(1L).build()
            val team2 = TeamBuilder().withId(2L).build()

            assertNotEquals(team1, team2, "Teams with different IDs should not be equal")
        }

        @Test
        fun `a Team is not equal to an object of a different type`() {
            val team = TeamBuilder().withId(1L).build()
            val otherObject = Any()

            assertNotEquals(team, otherObject, "Team should not be equal to a different type")
        }

        @Test
        fun `a Team is not equal to null`() {
            val team = TeamBuilder().withId(1L).build()

            assertNotEquals(null, team, "Team should not be equal to null")
        }

        @Test
        fun `a Team is equal to itself`() {
            val team = TeamBuilder().withId(1L).build()

            assertEquals(team, team, "Team should be equal to itself")
        }
    }

    @Nested
    @DisplayName("Player List Management Tests")
    inner class PlayerManagementTests {

        @Test
        fun `the TeamBuilder correctly adds a player`() {
            val player = PlayerBuilder().withId(101L).build()
            val team = TeamBuilder().withId(1L).withPlayer(player).build()

            assertEquals(1, team.players.size)
            assertTrue(team.players.contains(player))
        }

        @Test
        fun `the TeamBuilder correctly sets the bidirectional relationship`() {
            val player = PlayerBuilder().withId(101L).build()
            val team = TeamBuilder().withId(1L).withPlayer(player).build()

            assertNotNull(player.team, "The builder should have set the player\'s team reference")
            assertEquals(team, player.team, "The player\'s team reference should be the newly built team")
        }

        @Test
        fun `removing a player from a team's player list works`() {
            val player = PlayerBuilder().withId(101L).build()
            val team = TeamBuilder().withId(1L).withPlayer(player).build()

            assertEquals(1, team.players.size)

            team.players.remove(player)

            assertTrue(team.players.isEmpty(), "The players list should be empty after removing the player")
        }

        @Test
        fun `clearing the player list works`() {
            val player1 = PlayerBuilder().withId(101L).build()
            val player2 = PlayerBuilder().withId(102L).build()
            val team = TeamBuilder().withId(1L).withPlayers(listOf(player1, player2)).build()

            assertEquals(2, team.players.size)

            team.players.clear()

            assertTrue(team.players.isEmpty(), "The players list should be empty after being cleared")
        }
    }
}