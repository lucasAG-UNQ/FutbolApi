package com.grupob.futbolapi.services.implementation

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
        var team = teamRepository.findByWhoscoredIdWithPlayers(teamId)
        if (team == null) {
            team = scraperService.getTeam(teamId)
            // You might want to save the scraped team to your database here
            // teamRepository.save(team)
            println("${Calendar.getInstance().time} - After scraping")
        }
        return team
    }
}