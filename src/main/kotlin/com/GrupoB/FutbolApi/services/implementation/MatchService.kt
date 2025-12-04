package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.model.Match
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.PlayerDTO
import com.grupob.futbolapi.model.dto.TeamComparisonDTO
import com.grupob.futbolapi.model.dto.TeamStatsDTO
import com.grupob.futbolapi.repositories.MatchRepository
import com.grupob.futbolapi.services.IMatchService
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
@Service
class MatchService(
    private val scraperService: IWhoScoredScraperService,
    private val teamService: ITeamService,
    private val matchRepository: MatchRepository
) : IMatchService {

    private data class CoreStats(
        val wins: Int,
        val draws: Int,
        val losses: Int,
        val goalsFor: Int,
        val goalsAgainst: Int,
        val longestWinStreak: Int
    )

    private data class ExternalTeamData(
        val founded: Int?,
        val venue: String?,
        val clubColors: String?
    )

    override fun getNextMatches(teamID: Long): List<MatchDTO> {
        val team = teamService.getTeamWithPlayers(teamID)

        val nextMatches = matchRepository.findNextMatchesByTeamId(teamID, LocalDate.now())

        if (nextMatches.isNotEmpty()) {
            return nextMatches.map { MatchDTO.fromModel(it) }
        }

        if (team.lastUpdatedMatches != null && team.lastUpdatedMatches!!.isAfter(LocalDateTime.now().minusDays(1))) {
            return emptyList()
        }

        val scrapedMatches = scraperService.getNextTeamMatches(teamID)

        val matchesToSave = scrapedMatches.map { matchDto ->
            val homeTeam = if (teamID == matchDto.homeTeam.teamID) team else teamService.getTeamWithPlayers(matchDto.homeTeam.teamID)
            val awayTeam = if (teamID == matchDto.awayTeam.teamID) team else teamService.getTeamWithPlayers(matchDto.awayTeam.teamID)

            Match(
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                date = matchDto.date,
                tournament = matchDto.tournament,
                homeScore = matchDto.homeScore,
                awayScore = matchDto.awayScore
            )
        }

        if (matchesToSave.isNotEmpty()) {
            team.lastUpdatedMatches = LocalDateTime.now()
            matchRepository.saveAll(matchesToSave)
        }

        return scrapedMatches.filter { m -> m.date.isAfter(LocalDate.now()) }
    }

    override fun getFinishedMatches(teamID: Long): List<MatchDTO> {
        return matchRepository.findFinishedMatchesByTeamId(teamID, LocalDate.now()).map { MatchDTO.fromModel(it) }
    }

    override fun getTeamStats(teamID: Long): TeamStatsDTO {
        val team = teamService.getTeamWithPlayers(teamID)
        val finishedMatches = loadFinishedMatchesForTeam(team)
        val coreStats = calculateCoreStats(teamID, finishedMatches)
        val externalData = fetchExternalTeamData(team.name)
        val mvp = findMvp(team)

        val totalMatches = finishedMatches.size
        val winPercentage = if (totalMatches > 0) (coreStats.wins.toDouble() / totalMatches) * 100 else 0.0
        val averageGoalsFor = if (totalMatches > 0) coreStats.goalsFor.toDouble() / totalMatches else 0.0
        val averageGoalsAgainst = if (totalMatches > 0) coreStats.goalsAgainst.toDouble() / totalMatches else 0.0

        return TeamStatsDTO(
            teamId = teamID,
            teamName = team.name,
            totalMatches = totalMatches,
            wins = coreStats.wins,
            draws = coreStats.draws,
            losses = coreStats.losses,
            winPercentage = winPercentage,
            goalsFor = coreStats.goalsFor,
            goalsAgainst = coreStats.goalsAgainst,
            averageGoalsFor = averageGoalsFor,
            averageGoalsAgainst = averageGoalsAgainst,
            longestWinStreak = coreStats.longestWinStreak,
            founded = externalData.founded,
            venue = externalData.venue,
            clubColors = externalData.clubColors,
            mvp = mvp
        )
    }

    private fun loadFinishedMatchesForTeam(team: Team): List<Match> {
        var finishedMatches = matchRepository.findFinishedMatchesByTeamId(team.id!!, LocalDate.now())
        if (finishedMatches.isEmpty() || team.lastUpdatedMatches == null || team.lastUpdatedMatches!!.isBefore(LocalDateTime.now().minusDays(1))) {
            getNextMatches(team.id!!)
            finishedMatches = matchRepository.findFinishedMatchesByTeamId(team.id!!, LocalDate.now())
        }
        return finishedMatches
    }

    private fun calculateCoreStats(teamID: Long, finishedMatches: List<Match>): CoreStats {
        var wins = 0
        var draws = 0
        var losses = 0
        var goalsFor = 0
        var goalsAgainst = 0
        var currentStreak = 0
        var longestWinStreak = 0

        for (match in finishedMatches.sortedBy { it.date }) {
            val isHomeTeam = match.homeTeam?.id == teamID
            val won = if (isHomeTeam) (match.homeScore ?: 0) > (match.awayScore ?: 0) else (match.awayScore ?: 0) > (match.homeScore ?: 0)

            if (won) {
                currentStreak++
            } else {
                if (currentStreak > longestWinStreak) {
                    longestWinStreak = currentStreak
                }
                currentStreak = 0
            }

            if (isHomeTeam) {
                goalsFor += match.homeScore ?: 0
                goalsAgainst += match.awayScore ?: 0
                if (match.homeScore != null && match.awayScore != null) {
                    when {
                        match.homeScore!! > match.awayScore!! -> wins++
                        match.homeScore!! < match.awayScore!! -> losses++
                        else -> draws++
                    }
                }
            } else {
                goalsFor += match.awayScore ?: 0
                goalsAgainst += match.homeScore ?: 0
                if (match.homeScore != null && match.awayScore != null) {
                    when {
                        match.awayScore!! > match.homeScore!! -> wins++
                        match.awayScore!! < match.homeScore!! -> losses++
                        else -> draws++
                    }
                }
            }
        }
        if (currentStreak > longestWinStreak) {
            longestWinStreak = currentStreak
        }
        return CoreStats(wins, draws, losses, goalsFor, goalsAgainst, longestWinStreak)
    }

    private fun fetchExternalTeamData(teamName: String): ExternalTeamData {
        val footballDataTeam = teamService.getTeamFromFootballDataApi(teamName)
        val founded = footballDataTeam?.optInt("founded")
        val venue = footballDataTeam?.optString("venue")
        val clubColors = footballDataTeam?.optString("clubColors")
        return ExternalTeamData(founded, venue, clubColors)
    }

    private fun findMvp(team: Team): PlayerDTO? {
        return team.players.maxByOrNull { p -> p.rating ?: Double.MIN_VALUE }?.let { PlayerDTO.fromModel(it) }
    }

    override fun compareTeams(teamAId: Long, teamBId: Long): TeamComparisonDTO {
        val teamAStats = getTeamStats(teamAId)
        val teamBStats = getTeamStats(teamBId)
        return TeamComparisonDTO(teamA = teamAStats, teamB = teamBStats)
    }
}
