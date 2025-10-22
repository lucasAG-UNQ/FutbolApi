package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.TeamDTO
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/teams")
class TeamController(
    private val teamService: ITeamService,
    private val scraperService: IWhoScoredScraperService
) {

    private val logger = LoggerFactory.getLogger(TeamController::class.java)

    @GetMapping("/{teamID}")
    fun getTeamWithPlayers(@PathVariable teamID: Long): ResponseEntity<Any> {
        logger.info("GET /api/teams/{} received", teamID)

        val team = teamService.getTeamWithPlayers(teamID)
        logger.info("Service returned team: {}", team)

        return if (team != null) {
            val teamDTO = TeamDTO.fromModel(team)
            logger.info("Sending response DTO: {}", teamDTO)
            ResponseEntity.ok(teamDTO)
        } else {
            logger.warn("Team with id '{}' not found", teamID)
            ResponseEntity.status(404).body("Team not found")
        }
    }

    @GetMapping("/{teamID}/nextMatches")
    fun getNextMatches(@PathVariable teamID: Long): ResponseEntity<List<MatchDTO>> {
        val matches = scraperService.getNextTeamMatches(teamID)
        return ResponseEntity.ok(matches)
    }

    @GetMapping("/search/{searchParam}")
    fun searchTeams(@PathVariable searchParam: String): ResponseEntity<Any> {
        val search = scraperService.searchTeams(searchParam)
        return if (search.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(search)
        }
    }
}