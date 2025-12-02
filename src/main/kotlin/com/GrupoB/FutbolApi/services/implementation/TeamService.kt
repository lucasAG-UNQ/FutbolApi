package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.PredictionDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.repositories.TeamRepository
import com.grupob.futbolapi.services.IFootballDataApi
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.json.JSONObject
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.exp
import kotlin.math.pow

@Transactional(noRollbackFor = [TeamNotFoundException::class])
@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val scraperService: IWhoScoredScraperService,
    private val playerService: IPlayerService,
    private val footballDataApi: IFootballDataApi
) : ITeamService {

    override fun getTeamWithPlayers(teamName: String): Team {
        val team = teamRepository.findByNameWithPlayers(teamName)
            ?: throw TeamNotFoundException("Team with name $teamName not found")
        team.players = mergePlayerStats(team.players)
        return team
    }

    @Transactional(noRollbackFor = [TeamNotFoundException::class])
    override fun getTeamWithPlayers(teamId: Long): Team {
        val existingTeam = teamRepository.findByIdWithPlayers(teamId)

        if (existingTeam != null && existingTeam.lastUpdated.isAfter(LocalDateTime.now().minusDays(1))) {
            existingTeam.players = mergePlayerStats(existingTeam.players)
            return existingTeam
        }

        val scrapedTeam = try {
            scraperService.getTeam(teamId)
        } catch (e: TeamNotFoundException) {
            throw TeamNotFoundException("Team with id $teamId not found")
        }

        val teamToSave = scrapedTeam.toModel()
        teamToSave.players = mergePlayerStats(teamToSave.players)
        teamToSave.lastUpdated = LocalDateTime.now()

        val savedTeam = teamRepository.save(teamToSave)

        return savedTeam
    }

    private fun mergePlayerStats(players: MutableList<Player>): MutableList<Player> {
        if (players.isEmpty()) {
            return mutableListOf()
        }

        val mergedPlayers = players.groupBy { it.id }
            .map { (_, playerList) ->
                if (playerList.size > 1) {
                    val firstPlayer = playerList.first()
                    val ratings = playerList.mapNotNull { it.rating }.filter { it > 0 }
                    val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0

                    val mergedPlayer = Player(
                        id = firstPlayer.id,
                        name = firstPlayer.name,
                        position = firstPlayer.position,
                        age = firstPlayer.age,
                        tournament = playerList.joinToString(", ") { it.tournament ?: "" },
                        season = playerList.joinToString(", ") { it.season ?: "" },
                        apps = playerList.sumOf { it.apps ?: 0 },
                        goals = playerList.sumOf { it.goals ?: 0 },
                        assists = playerList.sumOf { it.assists ?: 0 },
                        rating = averageRating,
                        minutes = playerList.sumOf { it.minutes ?: 0 },
                        yellowCards = playerList.sumOf { it.yellowCards ?: 0 },
                        redCards = playerList.sumOf { it.redCards ?: 0 },
                        team = firstPlayer.team
                    )
                    mergedPlayer
                } else {
                    playerList.first()
                }
            }
        return mergedPlayers.toMutableList()
    }


    override fun predictMatch(teamA: Long, teamB: Long): PredictionDTO {
        val homeTeam = getTeamWithPlayers(teamA)
        val awayTeam = getTeamWithPlayers(teamB)

        val strengthA = calculateTeamStrength(homeTeam)
        val strengthB = calculateTeamStrength(awayTeam)

        // --- Probability Calculation ---
        val maxDrawChance = 0.34 // Max probability for a draw is 34%
        val drawFactor = 0.2     // Controls sensitivity to strength difference

        val strengthRatio = if (strengthB > 0) strengthA / strengthB else 1.0

        val drawProbability = maxDrawChance * exp(-(strengthRatio - 1).pow(2) / drawFactor)

        val remainingProbability = 1 - drawProbability
        val totalStrength = strengthA + strengthB

        val winProbabilityA = if (totalStrength > 0) remainingProbability * (strengthA / totalStrength) else 0.0
        val winProbabilityB = if (totalStrength > 0) remainingProbability * (strengthB / totalStrength) else 0.0

        val predictedWinner = when {
            winProbabilityA > winProbabilityB -> SimpleTeamDTO.fromModel(homeTeam)
            winProbabilityB > winProbabilityA -> SimpleTeamDTO.fromModel(awayTeam)
            else -> null // Indicates a predicted draw
        }

        return PredictionDTO(
            homeTeam = SimpleTeamDTO.fromModel(homeTeam),
            awayTeam = SimpleTeamDTO.fromModel(awayTeam),
            winProbabilityHomeTeam = winProbabilityA,
            winProbabilityAwayTeam = winProbabilityB,
            drawProbability = drawProbability,
            predictedWinner = predictedWinner
        )
    }

    private fun calculateTeamStrength(team: Team): Double {
        val players = team.players
        if (players.isNullOrEmpty()) {
            return 0.0
        }

        val totalMinutes = players.sumOf { it.minutes ?: 0 }
        if (totalMinutes == 0) {
            // Fallback to simple average if no minutes data is available
            val ratings = players.mapNotNull { it.rating }.filter { it > 0 }
            return if (ratings.isNotEmpty()) ratings.average() else 0.0
        }

        val weightedRatingSum = players.sumOf { (it.rating ?: 0.0) * (it.minutes ?: 0) }

        return weightedRatingSum / totalMinutes
    }

    override fun getTeamFromFootballDataApi(query: String): JSONObject? {
        return footballDataApi.getTeam(query)
    }
}
