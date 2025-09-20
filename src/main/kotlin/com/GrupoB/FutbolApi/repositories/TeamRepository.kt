package com.grupob.futbolapi.repositories

import com.grupob.futbolapi.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TeamRepository : JpaRepository<Team, Long> {
    fun findByName(name: String): Team?

    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.players WHERE t.name = :name")
    fun findByNameWithPlayers(name: String): Team?
}