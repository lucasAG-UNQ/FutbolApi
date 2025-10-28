package com.grupob.futbolapi.unit.security

import com.grupob.futbolapi.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authentication: Authentication
    private lateinit var token: String

    private val testSecret = "thisIsASecretKeyForTestingThatIsLongEnough"
    private val testExpiration: Long = 3600000 // 1 hour
    private val testUsername = "testuser"

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(testSecret, testExpiration)
        val userDetails = User.withUsername(testUsername).password("password").authorities("USER").build()
        authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        token = jwtTokenProvider.generateToken(authentication)
    }

    @Test
    fun shouldGenerateAValidToken() {
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun shouldExtractTheCorrectUsernameFromAValidToken() {
        val extractedUsername = jwtTokenProvider.getUsernameFromToken(token)
        assertEquals(testUsername, extractedUsername)
    }

    @Test
    fun shouldValidateACorrectAndUnexpiredToken() {
        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun shouldFailValidationForAnExpiredToken() {
        // Arrange
        val expiredProvider = JwtTokenProvider(testSecret, -1000) // Negative expiration
        val expiredToken = expiredProvider.generateToken(authentication)

        // Act & Assert
        assertFalse(jwtTokenProvider.validateToken(expiredToken))
    }

    @Test
    fun shouldFailValidationForATokenSignedWithADifferentKey() {
        // Arrange
        val otherSecret = "anotherSecretKeyThatIsAlsoLongEnoughForTesting"
        val otherProvider = JwtTokenProvider(otherSecret, testExpiration)
        val tokenFromOtherProvider = otherProvider.generateToken(authentication)

        // Act & Assert
        assertFalse(jwtTokenProvider.validateToken(tokenFromOtherProvider))
    }

    @Test
    fun shouldFailValidationForAMalformedToken() {
        // Arrange
        val malformedToken = "this.is.not.a.jwt"

        // Act & Assert
        assertFalse(jwtTokenProvider.validateToken(malformedToken))
    }

    @Test
    fun shouldFailValidationForATokenWithAnInvalidSignature() {
        // Arrange
        val invalidToken = token.take(token.lastIndexOf('.')) + ".invalidSignature"

        // Act & Assert
        assertFalse(jwtTokenProvider.validateToken(invalidToken))
    }
}