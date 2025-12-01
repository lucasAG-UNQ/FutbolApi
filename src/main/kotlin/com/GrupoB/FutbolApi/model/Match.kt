package com.grupob.futbolapi.model

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "matches")
class Match(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    var homeTeam: Team?,

    @ManyToOne
    var awayTeam: Team?,

    var date: LocalDate,

    var tournament: String?,

    var homeScore: Int?,

    var awayScore: Int?
)