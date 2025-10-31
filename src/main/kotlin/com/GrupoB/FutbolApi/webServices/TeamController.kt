package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.Request
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.TeamDTO
import com.grupob.futbolapi.repositories.RequestRepository
import com.grupob.futbolapi.repositories.UserRepository
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import io.swagger.v3.oas.annotations.Operation
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/teams")
class TeamController(
    private val teamService: ITeamService,
    private val scraperService: IWhoScoredScraperService,
    private val userRepository: UserRepository,
    private val requestRepository: RequestRepository
) {

    private val logger = LoggerFactory.getLogger(TeamController::class.java)

    private fun saveRequest(endpoint: String) {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
        if (user != null) {
            val request = Request(
                endpoint = endpoint,
                timestamp = LocalDateTime.now(),
                user = user
            )
            requestRepository.save(request)
        }
    }

    @GetMapping("/{teamID}")
    @Operation(summary = "Gets a team with its players by team ID")
    fun getTeamWithPlayers(@PathVariable teamID: Long): ResponseEntity<Any> {
        saveRequest("/api/teams/{teamID}")
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
    @Operation(summary = "Gets the next matches for a team by team ID")
    fun getNextMatches(@PathVariable teamID: Long): ResponseEntity<List<MatchDTO>> {
        saveRequest("/api/teams/{teamID}/nextMatches")
        val matches = scraperService.getNextTeamMatches(teamID)
        return ResponseEntity.ok(matches)
    }

    @GetMapping("/search/{searchParam}")
    @Operation(summary = "Searches for teams by a search parameter")
    fun searchTeams(@PathVariable searchParam: String): ResponseEntity<Any> {
        saveRequest("/api/teams/search/{searchParam}")
        val search = scraperService.searchTeams(searchParam)
        return if (search.isEmpty()) {
            ResponseEntity.status(404).body("No teams found matching '${searchParam}'")
        } else {
            ResponseEntity.ok(search)
        }
    }

    @GetMapping("/predict/{teamA}/{teamB}")
    @Operation(summary = "Predicts the winner of a match between two teams")
    fun predictMatch(@PathVariable teamA: Long, @PathVariable teamB: Long): ResponseEntity<Any>{
        saveRequest("/api/teams/predict/{teamA}/{teamB}")
        return try {
            val winningTeam = teamService.predictMatch(teamA,teamB)
            ResponseEntity.ok().body(TeamDTO.fromModel(winningTeam!!))
        } catch (e : TeamNotFoundException){
            ResponseEntity.status(404).body(e.message)
        }
    }
}