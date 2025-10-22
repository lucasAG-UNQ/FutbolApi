package com.grupob.futbolapi.model

import com.grupob.futbolapi.model.builder.PlayerBuilder
import com.grupob.futbolapi.model.builder.TeamBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Player Model Tests")
class PlayerTest {

    @Test
    fun `a Player can be created with all valid data`() {
        val player = PlayerBuilder()
            .withId(1L)
            .withName("Test Player")
            .withPosition("Midfielder")
            .withTournament("La Liga")
            .withSeason("2023/2024")
            .withApps(10)
            .withGoals(2)
            .withAssists(3)
            .withRating(7.5)
            .withMinutes(900)
            .withYellowCards(1)
            .withRedCards(0)
            .withAge(25)
            .build()

        assertEquals(1L, player.id)
        assertEquals("Test Player", player.name)
        assertEquals("Midfielder", player.position)
        assertEquals("La Liga", player.tournament)
        assertEquals(25, player.age)
        assertEquals(7.5, player.rating)
        assertNull(player.team, "Team should be null if not provided")
    }

    @Test
    fun `a Player can be created with only required data`() {
        val player = PlayerBuilder()
            .withName("Minimal Player")
            .build()

        assertNull(player.id)
        assertEquals("Minimal Player", player.name)
        // Assertions for default builder values
        assertEquals("A Position", player.position)
        assertEquals(20, player.age)
        assertEquals(0.0, player.rating)
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    inner class EqualityTests {

        @Test
        fun `two Player instances with different ids are not equal`() {
            val player1 = PlayerBuilder().withId(1L).build()
            val player2 = PlayerBuilder().withId(2L).build()

            assertNotEquals(player1, player2, "Players with different IDs should not be equal")
        }

        @Test
        fun `a Player is not equal to an object of a different type`() {
            val player = PlayerBuilder().withId(1L).build()
            val otherObject = Any()

            assertNotEquals(player, otherObject, "Player should not be equal to a different type")
        }

        @Test
        fun `a Player is not equal to null`() {
            val player = PlayerBuilder().withId(1L).build()

            assertNotEquals(null, player, "Player should not be equal to null")
        }

        @Test
        fun `a Player is equal to itself`() {
            val player = PlayerBuilder().withId(1L).build()

            assertEquals(player, player, "Player should be equal to itself")
        }
    }

    @Nested
    @DisplayName("Team Relationship Tests")
    inner class TeamRelationshipTests {

        @Test
        fun `a Player can be associated with a Team`() {
            val team = TeamBuilder().withId(10L).withName("Test United").build()
            val player = PlayerBuilder().withId(1L).withName("Test Player").build()

            // Associate player with team
            player.team = team

            assertNotNull(player.team, "Player's team should not be null after association")
            assertEquals(team, player.team, "Player's team should be the associated team")
            assertEquals(10L, player.team?.id)
        }

        @Test
        fun `a Player's team can be changed`() {
            val initialTeam = TeamBuilder().withId(10L).withName("Initial FC").build()
            val newTeam = TeamBuilder().withId(20L).withName("New FC").build()
            val player = PlayerBuilder().withId(1L).withTeam(initialTeam).build()

            // Change the team
            player.team = newTeam

            assertEquals(newTeam, player.team, "Player's team should be updated to the new team")
            assertNotEquals(initialTeam, player.team)
        }

        @Test
        fun `a Player's team can be set to null`() {
            val team = TeamBuilder().withId(10L).build()
            val player = PlayerBuilder().withId(1L).withTeam(team).build()

            // Remove association
            player.team = null

            assertNull(player.team, "Player's team should be null after removing the association")
        }
    }
}