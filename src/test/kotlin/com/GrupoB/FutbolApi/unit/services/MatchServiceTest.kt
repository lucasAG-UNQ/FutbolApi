package com.grupob.futbolapi.unit.services

import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.model.dto.TeamComparisonDTO
import com.grupob.futbolapi.repositories.MatchRepository
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import com.grupob.futbolapi.services.implementation.MatchService
import com.grupob.futbolapi.unit.model.builder.MatchBuilder
import com.grupob.futbolapi.unit.model.builder.TeamBuilder
import com.grupob.futbolapi.unit.model.builder.TeamStatsDTOBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class MatchServiceTest {

    @Mock
    private lateinit var scraperService: IWhoScoredScraperService

    @Mock
    private lateinit var teamService: ITeamService

    @Mock
    private lateinit var matchRepository: MatchRepository

    @InjectMocks
    private lateinit var matchService: MatchService

    private val teamId = 1L
    private val team = TeamBuilder().withId(teamId).withName("Test Team").build()

    @Nested
    @DisplayName("getNextMatches")
    inner class GetNextMatches {
        @BeforeEach
        fun setUp() {
            `when`(teamService.getTeamWithPlayers(teamId)).thenReturn(team)
        }

        @Test
        fun shouldReturnMatchesFromRepositoryIfTheyExist() {
            val matches = listOf(MatchBuilder().build())
            `when`(matchRepository.findNextMatchesByTeamId(teamId, LocalDate.now())).thenReturn(matches)

            val result = matchService.getNextMatches(teamId)

            assertEquals(1, result.size)
            verify(scraperService, never()).getNextTeamMatches(teamId)
        }

        @Test
        fun shouldReturnScrapedMatchesIfNoneExistInRepository() {
            val scrapedMatches = listOf(
                MatchDTO(1, SimpleTeamDTO(1, "Team A"), SimpleTeamDTO(2, "Team B"), LocalDate.now().plusDays(1), "La Liga", 1, 0)
            )
            `when`(matchRepository.findNextMatchesByTeamId(teamId, LocalDate.now())).thenReturn(emptyList())
            `when`(scraperService.getNextTeamMatches(teamId)).thenReturn(scrapedMatches)

            val result = matchService.getNextMatches(teamId)

            assertEquals(1, result.size)
        }

        @Test
        fun shouldNotScrapeIfTeamWasRecentlyUpdated() {
            team.lastUpdatedMatches = LocalDateTime.now()
            `when`(matchRepository.findNextMatchesByTeamId(teamId, LocalDate.now())).thenReturn(emptyList())

            val result = matchService.getNextMatches(teamId)

            assertEquals(0, result.size)
            verify(scraperService, never()).getNextTeamMatches(teamId)
        }
    }

    @Nested
    @DisplayName("getFinishedMatches")
    inner class GetFinishedMatches {
        @Test
        fun shouldReturnFinishedMatchesFromRepository() {
            val matches = listOf(MatchBuilder().build())
            `when`(matchRepository.findFinishedMatchesByTeamId(teamId, LocalDate.now())).thenReturn(matches)

            val result = matchService.getFinishedMatches(teamId)

            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("getTeamStats")
    inner class GetTeamStats {
        @BeforeEach
        fun setUp() {
            `when`(teamService.getTeamWithPlayers(teamId)).thenReturn(team)
        }

        @Test
        fun shouldCalculateTeamStatsCorrectly() {
            val matches = listOf(
                MatchBuilder().withHomeTeam(team).withAwayTeam(TeamBuilder().withId(2L).build()).withHomeScore(2).withAwayScore(1).build(), // Win
                MatchBuilder().withHomeTeam(TeamBuilder().withId(3L).build()).withAwayTeam(team).withHomeScore(1).withAwayScore(1).build()  // Draw
            )
            `when`(matchRepository.findFinishedMatchesByTeamId(teamId, LocalDate.now())).thenReturn(matches)
            `when`(teamService.getTeamFromFootballDataApi(any())).thenReturn(null)


            val result = matchService.getTeamStats(teamId)

            assertEquals(1, result.wins)
            assertEquals(1, result.draws)
            assertEquals(0, result.losses)
            assertEquals(3, result.goalsFor)
            assertEquals(2, result.goalsAgainst)
            assertEquals(50.0, result.winPercentage)
            assertEquals(1.5, result.averageGoalsFor)
            assertEquals(1.0, result.averageGoalsAgainst)
            assertEquals(1, result.longestWinStreak)
        }

        @Test
        fun shouldCalculateTeamStatsWithVariousMatchOutcomesAndStreaks() {
            val opponentTeam1 = TeamBuilder().withId(2L).build()
            val opponentTeam2 = TeamBuilder().withId(3L).build()
            val opponentTeam3 = TeamBuilder().withId(4L).build()
            val opponentTeam4 = TeamBuilder().withId(5L).build()

            val matches = listOf(
                // Win (Home) - Streak starts
                MatchBuilder().withHomeTeam(team).withAwayTeam(opponentTeam1).withHomeScore(3).withAwayScore(1).withDate(LocalDate.now().minusDays(5)).build(),
                // Win (Away) - Streak continues
                MatchBuilder().withHomeTeam(opponentTeam2).withAwayTeam(team).withHomeScore(0).withAwayScore(2).withDate(LocalDate.now().minusDays(4)).build(),
                // Draw (Home) - Streak breaks, longest streak is 2
                MatchBuilder().withHomeTeam(team).withAwayTeam(opponentTeam3).withHomeScore(1).withAwayScore(1).withDate(LocalDate.now().minusDays(3)).build(),
                // Loss (Away) - Streak is 0
                MatchBuilder().withHomeTeam(opponentTeam4).withAwayTeam(team).withHomeScore(2).withAwayScore(0).withDate(LocalDate.now().minusDays(2)).build(),
                // Win (Home) - Streak starts again
                MatchBuilder().withHomeTeam(team).withAwayTeam(opponentTeam1).withHomeScore(4).withAwayScore(0).withDate(LocalDate.now().minusDays(1)).build()
            )

            `when`(matchRepository.findFinishedMatchesByTeamId(teamId, LocalDate.now())).thenReturn(matches)
            `when`(teamService.getTeamFromFootballDataApi(any())).thenReturn(null)

            val result = matchService.getTeamStats(teamId)

            assertEquals(3, result.wins)
            assertEquals(1, result.draws)
            assertEquals(1, result.losses)
            assertEquals(10, result.goalsFor)
            assertEquals(4, result.goalsAgainst)
            assertEquals(60.0, result.winPercentage)
            assertEquals(2.0, result.averageGoalsFor)
            assertEquals(0.8, result.averageGoalsAgainst)
            assertEquals(2, result.longestWinStreak) // Longest streak was 2
        }
    }

    @Nested
    @DisplayName("compareTeams")
    inner class CompareTeams {
        @Test
        fun shouldReturnComparisonOfTwoTeams() {
            val teamAId = 1L
            val teamBId = 2L
            val teamA = TeamBuilder().withId(teamAId).withName("Team A").build()
            val teamB = TeamBuilder().withId(teamBId).withName("Team B").build()

            `when`(teamService.getTeamWithPlayers(teamAId)).thenReturn(teamA)
            `when`(teamService.getTeamWithPlayers(teamBId)).thenReturn(teamB)
            `when`(matchRepository.findFinishedMatchesByTeamId(eq(teamAId), any())).thenReturn(emptyList())
            `when`(matchRepository.findFinishedMatchesByTeamId(eq(teamBId), any())).thenReturn(emptyList())
            `when`(teamService.getTeamFromFootballDataApi(any())).thenReturn(null)


            val result = matchService.compareTeams(teamAId, teamBId)

            assertEquals("Team A", result.teamA.teamName)
            assertEquals("Team B", result.teamB.teamName)
        }
    }
}
