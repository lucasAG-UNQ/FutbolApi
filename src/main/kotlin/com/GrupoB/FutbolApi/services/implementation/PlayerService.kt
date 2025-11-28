package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.services.IPlayerService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Transactional
@Service
class PlayerService(private val playerRepository: PlayerRepository) : IPlayerService {
    @Transactional
    override fun save(player: Player): Player {
        return playerRepository.save(player)
    }

    @Transactional
    override fun saveAll(players: List<Player>): List<Player> {
        return playerRepository.saveAll(players)
    }

    override fun findByPlayerId(playerId : Long) : Player?{
        return playerRepository.findById(playerId).getOrNull()
    }
}