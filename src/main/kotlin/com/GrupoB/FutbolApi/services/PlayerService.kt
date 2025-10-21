package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player
import com.grupob.futbolapi.repositories.PlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerService(private val playerRepository: PlayerRepository) {
    @Transactional
    fun saveAll(players: List<Player>) {
        playerRepository.saveAll(players)
    }
}