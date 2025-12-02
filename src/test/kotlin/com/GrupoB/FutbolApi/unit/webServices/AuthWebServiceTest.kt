package com.grupob.futbolapi.unit.webServices

import com.fasterxml.jackson.databind.ObjectMapper
import com.grupob.futbolapi.model.User
import com.grupob.futbolapi.model.dto.LoginRequest
import com.grupob.futbolapi.model.dto.LoginResponse
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod

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
        // Create a dedicated exception handler for BadCredentialsException
        val exceptionHandler = object {
            @org.springframework.web.bind.annotation.ExceptionHandler(BadCredentialsException::class)
            fun handleAuthException(e: Exception): org.springframework.http.ResponseEntity<Unit> {
                return org.springframework.http.ResponseEntity.status(401).build()
            }
        }

        // Create a resolver that knows how to use our exception handler
        val resolver = ExceptionHandlerExceptionResolver()
        resolver.afterPropertiesSet() // Manually initialize it

        mockMvc = MockMvcBuilders.standaloneSetup(authWebService)
            .setHandlerExceptionResolvers(resolver) // Register the resolver
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
}
