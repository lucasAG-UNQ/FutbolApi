package com.grupob.futbolapi.unit.model

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.unit.model.builder.PlayerBuilder
import com.grupob.futbolapi.unit.model.builder.TeamBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Team Model Tests")
class TeamTest {

    @Test
    fun aTeamCanBeCreatedWithValidData() {
        val team = TeamBuilder()
            .withId(1L)
            .withName("Test Team")
            .build()

        assertEquals(1L, team.id)
        assertEquals("Test Team", team.name)
        assertTrue(team.players.isEmpty(), "A new team should have no players initially")
    }

    @Test
    fun aTeamCanBeCreatedWithANullId() {
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
        fun twoTeamInstancesWithDifferentIdsAreNotEqual() {
            val team1 = TeamBuilder().withId(1L).build()
            val team2 = TeamBuilder().withId(2L).build()

            assertNotEquals(team1, team2, "Teams with different IDs should not be equal")
        }

        @Test
        fun aTeamIsNotEqualToAnObjectOfADifferentType() {
            val team = TeamBuilder().withId(1L).build()
            val otherObject = Any()

            assertNotEquals(team, otherObject, "Team should not be equal to a different type")
        }

        @Test
        fun aTeamIsNotEqualToNull() {
            val team = TeamBuilder().withId(1L).build()

            assertNotEquals(null, team, "Team should not be equal to null")
        }

        @Test
        fun aTeamIsEqualToItself() {
            val team = TeamBuilder().withId(1L).build()

            assertEquals(team, team, "Team should be equal to itself")
        }
    }

    @Nested
    @DisplayName("Player List Management Tests")
    inner class PlayerManagementTests {

        private lateinit var team: Team
        private lateinit var player1: Player
        private lateinit var player2: Player

        @BeforeEach
        fun setUp() {
            team = TeamBuilder().withId(1L).build()
            player1 = PlayerBuilder().withId(101L).build()
            player2 = PlayerBuilder().withId(102L).build()
        }

        @Test
        fun theTeamBuilderCorrectlyAddsAPlayer() {
            // Act
            val teamWithPlayer = TeamBuilder().withId(1L).withPlayer(player1).build()

            // Assert
            assertEquals(1, teamWithPlayer.players.size)
            assertTrue(teamWithPlayer.players.contains(player1))
        }

        @Test
        fun theTeamBuilderCorrectlySetsTheBidirectionalRelationship() {
            // Act
            val teamWithPlayer = TeamBuilder().withId(1L).withPlayer(player1).build()

            // Assert
            assertNotNull(player1.team, "The builder should have set the player\'s team reference")
            assertEquals(teamWithPlayer, player1.team, "The player\'s team reference should be the newly built team")
        }

        @Test
        fun removingAPlayerFromATeamsPlayerListWorks() {
            // Arrange
            team.players.add(player1)
            assertEquals(1, team.players.size)

            // Act
            team.players.remove(player1)

            // Assert
            assertTrue(team.players.isEmpty(), "The players list should be empty after removing the player")
        }

        @Test
        fun clearingThePlayerListWorks() {
            // Arrange
            team.players.addAll(listOf(player1, player2))
            assertEquals(2, team.players.size)

            // Act
            team.players.clear()

            // Assert
            assertTrue(team.players.isEmpty(), "The players list should be empty after being cleared")
        }
    }
}