package com.devvikram.expensetracker.expensetracker.security

import com.devvikram.expensetracker.expensetracker.entity.User


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtUtil {

    @Value("\${jwt.secret:mySecretKeyForJwtTokenGenerationMustBe256BitsLongForHS256Algorithm}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400000}") // 24 hours in milliseconds
    private var expiration: Long = 86400000

    private fun getSigningKey(): Key {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /**
     * Generate JWT token for User entity
     */
    fun generateToken(user: User): String {
        val claims = HashMap<String, Any>()
        claims["userId"] = user.id
        claims["email"] = user.email
        claims["name"] = user.name
        claims["roles"] = user.getRoleNames().map { "ROLE_$it" }

        return createToken(claims, user.email)
    }

    /**
     * Generate JWT token for UserDetails
     */
    fun generateToken(userDetails: UserDetails): String {
        val claims = HashMap<String, Any>()
        claims["roles"] = userDetails.authorities.map { it.authority }

        return createToken(claims, userDetails.username)
    }

    /**
     * Create token with claims and subject
     */
    private fun createToken(claims: Map<String, Any>, subject: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * Extract username (email) from token
     */
    fun extractUsername(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }

    /**
     * Extract user ID from token
     */
    fun extractUserId(token: String): Long {
        val claims = extractAllClaims(token)
        return claims["userId"].toString().toLong()
    }

    /**
     * Extract roles from token
     */
    fun extractRoles(token: String): List<String> {
        val claims = extractAllClaims(token)
        @Suppress("UNCHECKED_CAST")
        return claims["roles"] as? List<String> ?: emptyList()
    }

    /**
     * Extract expiration date from token
     */
    fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    /**
     * Generic method to extract claim
     */
    fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    /**
     * Extract all claims from token
     */
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body
    }

    /**
     * Check if token is expired
     */
    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    /**
     * Validate token against UserDetails
     */
    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username && !isTokenExpired(token))
    }

    /**
     * Validate token (basic validation)
     */
    fun validateToken(token: String): Boolean {
        return try {
            !isTokenExpired(token)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get token expiration time in milliseconds
     */
    fun getExpirationTime(): Long = expiration
}