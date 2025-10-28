package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.TeamRepository
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Calendar

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val scraperService: IWhoScoredScraperService,
    private val playerService: IPlayerService
) : ITeamService {

    override fun getTeam(teamName: String): Team? {
        return teamRepository.findByNameWithPlayers(teamName)
    }

    override fun getTeamWithPlayers(teamName: String): Team? {
        return teamRepository.findByNameWithPlayers(teamName)
    }

    @Transactional
    override fun getTeamWithPlayers(teamId: Long): Team? {
        var team = teamRepository.findByIdWithPlayers(teamId)
        if (team == null) {
            team = try{
                scraperService.getTeam(teamId).toModel()
            } catch (e : TeamNotFoundException){
                null
            }
            // You might want to save the scraped team to your database here
            // teamRepository.save(team)
            println("${Calendar.getInstance().time} - After scraping")
        }
        return team
    }

    @Transactional
    override fun predictMatch(teamA: Long, teamB: Long): Team? {
        var teamHome:Team? = getTeamWithPlayers(teamA)
        var teamAway:Team? = getTeamWithPlayers(teamB)

        var res : Team?
        if (teamHome == null) throw TeamNotFoundException("Home team with id ${teamA} was not found")
        if (teamAway == null) throw TeamNotFoundException("Away team with id ${teamB} was not found")
        val homeTeamRating = teamHome.players.map { player ->  player.rating?:0.0 }.average()
        val awayTeamRating = teamAway.players.map { player ->  player.rating?:0.0 }.average()
        res = if (homeTeamRating >= awayTeamRating) teamHome else teamAway

        return res
    }
}