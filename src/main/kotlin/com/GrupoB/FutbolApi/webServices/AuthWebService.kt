package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.model.User
import com.grupob.futbolapi.repositories.UserRepository
import com.grupob.futbolapi.security.JwtTokenProvider
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String)
data class RegisterRequest(val username: String, val password: String)

@RestController
@RequestMapping("/api/auth")
class AuthWebService(
    private val authenticationManager: AuthenticationManager,
    private val tokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/register")
    fun registerUser(@RequestBody registerRequest: RegisterRequest): ResponseEntity<*> {
        if (userRepository.findByUsername(registerRequest.username) != null) {
            return ResponseEntity.badRequest().body("Username is already taken!")
        }

        val user = User(
            username = registerRequest.username,
            password_hash = passwordEncoder.encode(registerRequest.password)
        )
        userRepository.save(user)

        return ResponseEntity.ok("User registered successfully")
    }

    @PostMapping("/login")
    fun authenticateUser(@RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                loginRequest.username,
                loginRequest.password
            )
        )

        SecurityContextHolder.getContext().authentication = authentication
        val token = tokenProvider.generateToken(authentication)
        return ResponseEntity.ok(LoginResponse(token))
    }
}
