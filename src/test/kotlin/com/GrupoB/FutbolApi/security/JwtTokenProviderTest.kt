package com.grupob.futbolapi.security

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
    private val testSecret = "thisIsASecretKeyForTestingThatIsLongEnough"
    private val testExpiration: Long = 3600000 // 1 hour

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(testSecret, testExpiration)
    }

    private fun createTestAuthentication(username: String): Authentication {
        val userDetails = User.withUsername(username).password("password").authorities("USER").build()
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }

    @Test
    fun `should generate a valid token`() {
        val authentication = createTestAuthentication("testuser")
        val token = jwtTokenProvider.generateToken(authentication)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `should extract the correct username from a valid token`() {
        val username = "testuser"
        val authentication = createTestAuthentication(username)
        val token = jwtTokenProvider.generateToken(authentication)

        val extractedUsername = jwtTokenProvider.getUsernameFromToken(token)

        assertEquals(username, extractedUsername)
    }

    @Test
    fun `should validate a correct and unexpired token`() {
        val authentication = createTestAuthentication("testuser")
        val token = jwtTokenProvider.generateToken(authentication)

        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `should fail validation for an expired token`() {
        val expiredProvider = JwtTokenProvider(testSecret, -1000) // Negative expiration
        val authentication = createTestAuthentication("testuser")
        val expiredToken = expiredProvider.generateToken(authentication)

        assertFalse(jwtTokenProvider.validateToken(expiredToken))
    }

    @Test
    fun `should fail validation for a token signed with a different key`() {
        val otherSecret = "anotherSecretKeyThatIsAlsoLongEnoughForTesting"
        val otherProvider = JwtTokenProvider(otherSecret, testExpiration)
        val authentication = createTestAuthentication("testuser")
        val tokenFromOtherProvider = otherProvider.generateToken(authentication)

        assertFalse(jwtTokenProvider.validateToken(tokenFromOtherProvider))
    }

    @Test
    fun `should fail validation for a malformed token`() {
        val malformedToken = "this.is.not.a.jwt"

        assertFalse(jwtTokenProvider.validateToken(malformedToken))
    }

    @Test
    fun `should fail validation for a token with an invalid signature`() {
        val authentication = createTestAuthentication("testuser")
        val token = jwtTokenProvider.generateToken(authentication)
        val invalidToken = token.substring(0, token.lastIndexOf('.')) + ".invalidSignature"

        assertFalse(jwtTokenProvider.validateToken(invalidToken))
    }
}