package com.grupob.futbolapi.model

import com.grupob.futbolapi.model.builder.MatchBuilder
import com.grupob.futbolapi.model.builder.TeamBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Match Model Tests")
class MatchTest {

    @Test
    fun `a Match can be created with valid data`() {
        val homeTeam = TeamBuilder().withId(10L).withName("Real Madrid").build()
        val awayTeam = TeamBuilder().withId(20L).withName("Barcelona").build()
        val matchDate = LocalDate.of(2024, 10, 26)

        val match = MatchBuilder()
            .withId(1L)
            .withHomeTeam(homeTeam)
            .withAwayTeam(awayTeam)
            .withDate(matchDate)
            .withTournament("La Liga")
            .build()

        assertEquals(1L, match.id)
        assertEquals(homeTeam, match.homeTeam)
        assertEquals("Real Madrid", match.homeTeam?.name)
        assertEquals(awayTeam, match.awayTeam)
        assertEquals("Barcelona", match.awayTeam?.name)
        assertEquals(matchDate, match.date)
        assertEquals("La Liga", match.tournament)
    }

    @Test
    fun `a Match can be created with default builder values`() {
        val match = MatchBuilder().build()

        assertNull(match.id)
        assertNotNull(match.homeTeam)
        assertNotNull(match.awayTeam)
        assertEquals("Home Team", match.homeTeam?.name)
        assertEquals("Away Team", match.awayTeam?.name)
        assertEquals(LocalDate.now(), match.date)
        assertEquals("A Tournament", match.tournament)
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    inner class EqualityTests {

        @Test
        fun `two Match instances with different ids are not equal`() {
            val match1 = MatchBuilder().withId(1L).build()
            val match2 = MatchBuilder().withId(2L).build()

            assertNotEquals(match1, match2, "Matches with different IDs should not be equal")
        }

        @Test
        fun `a Match is not equal to an object of a different type`() {
            val match = MatchBuilder().withId(1L).build()
            val otherObject = Any()

            assertNotEquals(match, otherObject, "Match should not be equal to a different type")
        }

        @Test
        fun `a Match is not equal to null`() {
            val match = MatchBuilder().withId(1L).build()

            assertNotEquals(null, match, "Match should not be equal to null")
        }

        @Test
        fun `a Match is equal to itself`() {
            val match = MatchBuilder().withId(1L).build()

            assertEquals(match, match, "Match should be equal to itself")
        }
    }
}