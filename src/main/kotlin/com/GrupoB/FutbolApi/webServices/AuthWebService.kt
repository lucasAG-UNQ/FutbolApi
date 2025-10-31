package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.model.User
import com.grupob.futbolapi.model.dto.RequestDTO
import com.grupob.futbolapi.repositories.RequestRepository
import com.grupob.futbolapi.repositories.UserRepository
import com.grupob.futbolapi.security.JwtTokenProvider
import io.swagger.v3.oas.annotations.Operation
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
    private val passwordEncoder: PasswordEncoder,
    private val requestRepository: RequestRepository
) {

    @PostMapping("/register")
    @Operation(summary = "Registers a new user")
    fun registerUser(@RequestBody registerRequest: RegisterRequest): ResponseEntity<*> {
        if (userRepository.findByUsername(registerRequest.username) != null) {
            return ResponseEntity.badRequest().body("Username is already taken!")
        }

        val user = User(
            username = registerRequest.username,
            passwordHash = passwordEncoder.encode(registerRequest.password)
        )
        userRepository.save(user)

        return ResponseEntity.ok("User registered successfully")
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticates a user and returns a JWT token")
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

    @GetMapping("/history")
    @Operation(summary = "Gets the request history for the authenticated user")
    fun getRequestHistory(): ResponseEntity<List<RequestDTO>> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
        return if (user != null) {
            val requests = requestRepository.findByUserId(user.id!!)
            val requestDTOs = requests.map { RequestDTO(it.endpoint, it.timestamp.toString()) }
            ResponseEntity.ok(requestDTOs)
        } else {
            ResponseEntity.status(404).body(null)
        }
    }
}
