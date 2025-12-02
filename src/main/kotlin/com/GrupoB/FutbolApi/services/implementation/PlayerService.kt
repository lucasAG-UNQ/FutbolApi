package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.exceptions.PlayerNotFoundException
import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.services.IPlayerService
import com.grupob.futbolapi.services.IWhoScoredScraperService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Transactional
@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
    private val scraperService: IWhoScoredScraperService
) : IPlayerService {

    override fun save(player: Player): Player {
        return playerRepository.save(player)
    }

    override fun saveAll(players: List<Player>): List<Player> {
        return playerRepository.saveAll(players)
    }

    override fun findByPlayerId(playerId : Long) : Player?{
        val player = playerRepository.findById(playerId).getOrNull()
        if (player == null) {
            try {
                val scrapedPlayer = scraperService.getPlayerById(playerId)
                return savePlayer(scrapedPlayer)
            } catch (e: PlayerNotFoundException) {
                return null
            }
        }
        return player
    }

    override fun savePlayer(player: Player): Player {
        return playerRepository.save(player)
    }
}
