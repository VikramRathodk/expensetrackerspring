package com.devvikram.expensetracker.expensetracker.security



import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwt = extractJwtFromRequest(request)

        if (jwt == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val username = jwtUtil.extractUsername(jwt)

            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userDetailsService.loadUserByUsername(username)

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    val authentication = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }

            filterChain.doFilter(request, response)

        } catch (ex: ExpiredJwtException) {
            writeUnauthorized(response, "TOKEN_EXPIRED", "Access token has expired — use /api/v1/auth/refresh")
        } catch (ex: JwtException) {
            writeUnauthorized(response, "TOKEN_INVALID", "Invalid JWT token")
        }
    }

    private fun writeUnauthorized(response: HttpServletResponse, code: String, message: String) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """{"status":false,"code":"$code","message":"$message"}"""
        )
    }

    private fun extractJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}
