package com.grupob.futbolapi.repositories

import com.grupob.futbolapi.model.Match
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface MatchRepository : JpaRepository<Match, Long> {
    @Query("SELECT m FROM Match m WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) AND m.date >= :currentDate")
    fun findNextMatchesByTeamId(@Param("teamId") teamId: Long, @Param("currentDate") currentDate: LocalDate): List<Match>

    @Query("SELECT m FROM Match m WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) AND m.date < :currentDate")
    fun findFinishedMatchesByTeamId(@Param("teamId") teamId: Long, @Param("currentDate") currentDate: LocalDate): List<Match>
}
