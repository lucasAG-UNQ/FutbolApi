package com.grupob.futbolapi.config

import com.grupob.futbolapi.model.Request
import com.grupob.futbolapi.model.User
import com.grupob.futbolapi.repositories.RequestRepository
import com.grupob.futbolapi.repositories.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val requestRepository: RequestRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        if (userRepository.count() == 0L) {
            val user1 = User(
                username = "carlitos",
                passwordHash = passwordEncoder.encode("1234")
            )
            val user2 = User(
                username = "pepe",
                passwordHash = passwordEncoder.encode("1234")
            )
            userRepository.saveAll(listOf(user1, user2))

            val request1 = Request(
                endpoint = "/api/teams/1",
                timestamp = LocalDateTime.now(),
                user = user1
            )
            val request2 = Request(
                endpoint = "/api/teams/search/bayern",
                timestamp = LocalDateTime.now(),
                user = user1
            )
            val request3 = Request(
                endpoint = "/api/teams/predict/1/2",
                timestamp = LocalDateTime.now(),
                user = user2
            )
            requestRepository.saveAll(listOf(request1, request2, request3))
        }
    }
}
