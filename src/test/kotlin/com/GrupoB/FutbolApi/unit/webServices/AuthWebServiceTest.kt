package com.grupob.futbolapi.unit.webServices

import com.fasterxml.jackson.databind.ObjectMapper
import com.grupob.futbolapi.model.Request
import com.grupob.futbolapi.model.User
import com.grupob.futbolapi.model.dto.LoginRequest
import com.grupob.futbolapi.model.dto.RegisterRequest
import com.grupob.futbolapi.repositories.RequestRepository
import com.grupob.futbolapi.repositories.UserRepository
import com.grupob.futbolapi.security.JwtTokenProvider
import com.grupob.futbolapi.webServices.AuthWebService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthWebService Unit Tests")
class AuthWebServiceTest {

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var tokenProvider: JwtTokenProvider

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var requestRepository: RequestRepository

    @InjectMocks
    private lateinit var authWebService: AuthWebService

    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val exceptionHandler = object {
            @org.springframework.web.bind.annotation.ExceptionHandler(BadCredentialsException::class)
            fun handleAuthException(e: Exception): org.springframework.http.ResponseEntity<Unit> {
                return org.springframework.http.ResponseEntity.status(401).build()
            }
        }

        val resolver = ExceptionHandlerExceptionResolver()
        resolver.afterPropertiesSet()

        mockMvc = MockMvcBuilders.standaloneSetup(authWebService)
            .setHandlerExceptionResolvers(resolver)
            .build()
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    inner class RegisterUser {

        @Test
        fun shouldReturn200OKWhenRegistrationIsSuccessful() {
            val registerRequest = RegisterRequest("newUser", "password123")
            `when`(userRepository.findByUsername(registerRequest.username)).thenReturn(null)
            `when`(passwordEncoder.encode(registerRequest.password)).thenReturn("encodedPassword")
            `when`(userRepository.save(any(User::class.java))).thenReturn(User(1L, registerRequest.username, "encodedPassword"))

            mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk)
                .andExpect(content().string("User registered successfully"))
        }

        @Test
        fun shouldReturn400BadRequestWhenUsernameIsAlreadyTaken() {
            val registerRequest = RegisterRequest("existingUser", "password123")
            `when`(userRepository.findByUsername(registerRequest.username)).thenReturn(User(1L, registerRequest.username, "anypass"))

            mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest)
                .andExpect(content().string("Username is already taken!"))
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    inner class LoginUser {

        @Test
        fun shouldReturn200OKWithJwtWhenLoginIsSuccessful() {
            val loginRequest = LoginRequest("testuser", "password")
            val authentication: Authentication = UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            val jwt = "mock.jwt.token"

            `when`(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken::class.java))).thenReturn(authentication)
            `when`(tokenProvider.generateToken(authentication)).thenReturn(jwt)

            mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.token").value(jwt))
        }
    }

    @Nested
    @DisplayName("GET /api/auth/history")
    inner class GetRequestHistory {

        @Test
        fun shouldReturn200OKWithRequestHistoryForAuthenticatedUser() {
            val user = User(1L, "testuser", "password")
            val requests = listOf(
                Request(1L, "/api/teams/1", LocalDateTime.now(), user),
                Request(2L, "/api/teams/2", LocalDateTime.now(), user)
            )
            val authentication: Authentication = UsernamePasswordAuthenticationToken(user.username, null)
            SecurityContextHolder.getContext().authentication = authentication

            `when`(userRepository.findByUsername(user.username)).thenReturn(user)
            `when`(requestRepository.findByUserId(user.id!!)).thenReturn(requests)

            mockMvc.perform(get("/api/auth/history")
                .principal(authentication))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].endpoint").value("/api/teams/1"))
        }

        @Test
        fun shouldReturn404NotFoundWhenUserIsNotFound() {
            val authentication: Authentication = UsernamePasswordAuthenticationToken("nonexistentuser", null)
            SecurityContextHolder.getContext().authentication = authentication

            `when`(userRepository.findByUsername("nonexistentuser")).thenReturn(null)

            mockMvc.perform(get("/api/auth/history")
                .principal(authentication))
                .andExpect(status().isNotFound)
        }
    }
}
