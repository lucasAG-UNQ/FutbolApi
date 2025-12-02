package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.model.dto.PlayerPerformanceDTO
import com.grupob.futbolapi.services.IPlayerService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/players")
class PlayerController(
    private val playerService: IPlayerService
) {

    @GetMapping("/{playerID}/performance")
    @Operation(summary = "Gets a player's performance data by player ID")
    fun getPlayerPerformance(@PathVariable playerID: Long): ResponseEntity<Any> {
        val player = playerService.findByPlayerId(playerID)
        return if (player != null) {
            val performanceDTO = PlayerPerformanceDTO.fromModel(player)
            ResponseEntity.ok(performanceDTO)
        } else {
            ResponseEntity.status(404).body("Player not found")
        }
    }
}
