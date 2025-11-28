package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.exceptions.TeamNotFoundException
import com.grupob.futbolapi.model.dto.MatchDTO
import com.grupob.futbolapi.model.dto.TeamDTO
import com.grupob.futbolapi.services.ITeamService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import io.swagger.v3.oas.annotations.Operation
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
    @Operation(summary = "Gets a team with its players by team ID")
    fun getTeamWithPlayers(@PathVariable teamID: Long): ResponseEntity<Any> {

        val team = teamService.getTeamWithPlayers(teamID)

        return if (team != null) {
            val teamDTO = TeamDTO.fromModel(team)
            ResponseEntity.ok(teamDTO)
        } else {
            ResponseEntity.status(404).body("Team not found")
        }
    }

    @GetMapping("/{teamID}/nextMatches")
    @Operation(summary = "Gets the next matches for a team by team ID")
    fun getNextMatches(@PathVariable teamID: Long): ResponseEntity<List<MatchDTO>> {
        val matches = scraperService.getNextTeamMatches(teamID)
        return ResponseEntity.ok(matches)
    }

    @GetMapping("/search/{searchParam}")
    @Operation(summary = "Searches for teams by a search parameter")
    fun searchTeams(@PathVariable searchParam: String): ResponseEntity<Any> {
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
        return try {
            ResponseEntity.ok().body(teamService.predictMatch(teamA,teamB))
        } catch (e : TeamNotFoundException){
            ResponseEntity.status(404).body(e.message)
        }
    }
}
