package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Team
import com.grupob.futbolapi.repositories.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Calendar

@Service
class TeamService(private val teamRepository: TeamRepository,
                  val scraperService: WhoScoredScraperService,
                  val playerService: PlayerService
) {
    fun getTeam(teamName: String): Team? {
        return teamRepository.findByNameWithPlayers(teamName)
    }
    fun getTeamWithPlayers(teamName: String): Team? {
        return teamRepository.findByNameWithPlayers(teamName)
    }

    @Transactional
    fun getTeamWithPlayers(teamId: Long):Team?{
        var team=teamRepository.findByWhoscoredIdWithPlayers(teamId)
        if (team==null) {
            team=scraperService.getTeam(teamId)

            teamRepository.save(team)

            println("${Calendar.getInstance().time} - After saving")
        }
        return team
    }
}