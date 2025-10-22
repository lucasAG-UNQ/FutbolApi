package com.grupob.futbolapi.services.implementation

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.repositories.PlayerRepository
import com.grupob.futbolapi.services.IPlayerService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerService(private val playerRepository: PlayerRepository) : IPlayerService {
    @Transactional
    override fun saveAll(players: List<Player>) {
        playerRepository.saveAll(players)
    }
}