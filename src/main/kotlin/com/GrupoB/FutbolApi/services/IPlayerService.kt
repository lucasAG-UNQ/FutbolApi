package com.grupob.futbolapi.services

import com.grupob.futbolapi.model.Player

interface IPlayerService {
    fun saveAll(players: List<Player>)
}