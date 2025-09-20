package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.services.TeamService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/teams")
class TeamController(private val teamService: TeamService) {

    private val logger = LoggerFactory.getLogger(TeamController::class.java)

    @GetMapping("/{teamName}")
    fun getTeamWithPlayers(@PathVariable teamName: String): ResponseEntity<Any> {
        logger.info("GET /api/teams/{} received", teamName)

        val team = teamService.getTeamWithPlayers(teamName)
        logger.info("Service returned team: {}", team)

        return if (team != null) {
            val playerDTOs = team.players.map { player ->
                mapOf("name" to player.name, "position" to player.position)
            }
            val teamDTO = mapOf(
                "id" to team.id,
                "name" to team.name,
                "players" to playerDTOs
            )
            logger.info("Sending response DTO: {}", teamDTO)
            ResponseEntity.ok(teamDTO)
        } else {
            logger.warn("Team with name '{}' not found", teamName)
            ResponseEntity.notFound().build()
        }
    }
}