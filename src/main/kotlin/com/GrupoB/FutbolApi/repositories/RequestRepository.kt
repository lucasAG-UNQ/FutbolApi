package com.grupob.futbolapi.repositories

import com.grupob.futbolapi.model.Request
import org.springframework.data.jpa.repository.JpaRepository

interface RequestRepository : JpaRepository<Request, Long> {
    fun findByUserId(userId: Long): List<Request>
}
