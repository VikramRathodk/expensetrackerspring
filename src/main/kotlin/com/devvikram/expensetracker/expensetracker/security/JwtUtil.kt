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
class JwtUtil(

    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration:86400000}")
    private val expiration: Long

) {

    private fun getSigningKey(): Key =
        Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(user: User): String {
        val claims = HashMap<String, Any>()
        claims["userId"] = user.id
        claims["email"] = user.email
        claims["name"] = user.name
        claims["roles"] = user.getRoleNames().map { "ROLE_$it" }

        return createToken(claims, user.email)
    }

    fun generateToken(userDetails: UserDetails): String {
        val claims = HashMap<String, Any>()
        claims["roles"] = userDetails.authorities.map { it.authority }

        return createToken(claims, userDetails.username)
    }

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

    fun extractUsername(token: String): String =
        extractAllClaims(token).subject

    fun extractUserId(token: String): Long =
        extractAllClaims(token)["userId"].toString().toLong()

    fun extractRoles(token: String): List<String> =
        extractAllClaims(token)["roles"] as? List<String> ?: emptyList()

    private fun extractAllClaims(token: String): Claims =
        Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body

    fun validateToken(token: String, userDetails: UserDetails): Boolean =
        extractUsername(token) == userDetails.username
}
