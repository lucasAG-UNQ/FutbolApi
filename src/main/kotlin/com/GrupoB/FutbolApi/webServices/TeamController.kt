package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.model.Match
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.PlayerDTO
import com.grupob.futbolapi.model.dto.TeamDTO
import com.grupob.futbolapi.services.TeamService
import com.grupob.futbolapi.services.WhoScoredScraperService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.stream.Collectors


@RestController
@RequestMapping("/api/teams")
class TeamController(private val teamService: TeamService,
                     private val scraperService: WhoScoredScraperService
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
            logger.warn("Team with name '{}' not found", teamID)
            ResponseEntity.status(404).body("Team not found")
        }
    }

    @GetMapping("/{teamID}/nextMatches")
    fun getNextMatches(@PathVariable teamID: Long): ResponseEntity<Any> {
        val matches = scraperService.getNextTeamMatches(teamID)
        return ResponseEntity.ok(matches)
    }

    @GetMapping("/search/{searchParam}")
    fun searchTeams(@PathVariable searchParam: String): ResponseEntity<Any> {
        val search = scraperService.searchTeams(searchParam)
        val res:ResponseEntity<Any> =
            if (search.isEmpty())
                ResponseEntity.notFound().build()
            else
                ResponseEntity.ok(search)
        return res
    }
}