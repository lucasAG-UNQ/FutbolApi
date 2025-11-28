package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.model.dto.PredictionDTO
import com.grupob.futbolapi.model.dto.SimpleTeamDTO
import com.grupob.futbolapi.repositories.TeamRepository
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Calendar
import kotlin.math.exp
import kotlin.math.pow

@Transactional
@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val scraperService: IWhoScoredScraperService,
    private val playerService: IPlayerService
) : ITeamService {

    override fun getTeamWithPlayers(teamName: String): Team? {
        val team = teamRepository.findByNameWithPlayers(teamName)
        team?.players = mergePlayerStats(team?.players ?: mutableListOf())
        return team
    }

    override fun getTeamWithPlayers(teamId: Long): Team? {
        // First, try to fetch the team from our database.
        val existingTeam = teamRepository.findByIdWithPlayers(teamId)
        if (existingTeam != null) {
            existingTeam.players = mergePlayerStats(existingTeam.players)
            return existingTeam
        }

        // If not found, scrape it from the external service.
        val scrapedTeam = try {
            scraperService.getTeam(teamId)
        } catch (e: TeamNotFoundException) {
            return null // The team doesn't exist anywhere.
        }

        // Convert the DTO to a model and save it.
        val teamToSave = scrapedTeam.toModel()
        teamToSave.players = mergePlayerStats(teamToSave.players)

        // 1. Save the parent Team first to make it a managed entity.
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
                        rating = playerList.mapNotNull { it.rating }.average(),
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

    @Transactional
    override fun predictMatch(teamA: Long, teamB: Long): PredictionDTO {
        val homeTeam:Team? = getTeamWithPlayers(teamA)
        val awayTeam:Team? = getTeamWithPlayers(teamB)

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
            winProbabilityA > winProbabilityB -> SimpleTeamDTO.fromModel(homeTeam!!)
            winProbabilityB > winProbabilityA -> SimpleTeamDTO.fromModel(awayTeam!!)
            else -> null // Indicates a predicted draw
        }

        return PredictionDTO(
            homeTeam = SimpleTeamDTO.fromModel(homeTeam!!),
            awayTeam = SimpleTeamDTO.fromModel(awayTeam!!),
            winProbabilityHomeTeam = winProbabilityA,
            winProbabilityAwayTeam = winProbabilityB,
            drawProbability = drawProbability,
            predictedWinner = predictedWinner
        )
    }

    private fun calculateTeamStrength(team: Team?): Double {
        val players = team!!.players
        if (players.isNullOrEmpty()) {
            return 0.0
        }

        val totalMinutes = players.sumOf { it.minutes ?: 0 }
        if (totalMinutes == 0) {
            // Fallback to simple average if no minutes data is available
            return players.mapNotNull { it.rating }.average()
        }

        val weightedRatingSum = players.sumOf { (it.rating ?: 0.0) * (it.minutes ?: 0) }

        return weightedRatingSum / totalMinutes
    }
}
