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
