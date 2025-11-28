package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player

interface IPlayerService {
    fun save(player: Player): Player
    fun saveAll(players: List<Player>): List<Player>
    fun findByPlayerId(playerId: Long): Player?
}