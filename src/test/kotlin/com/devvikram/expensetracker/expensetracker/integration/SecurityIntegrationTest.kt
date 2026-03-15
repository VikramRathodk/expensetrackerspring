package com.devvikram.expensetracker.expensetracker.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    // ── Public endpoints — no auth required ───────────────────────────────────

    @Test
    fun `swagger-ui is accessible without authentication`() {
        mockMvc.get("/swagger-ui/index.html")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `openapi docs are accessible without authentication`() {
        mockMvc.get("/v3/api-docs")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `login endpoint with wrong credentials returns 401 (not filtered by Spring Security)`() {
        // The login endpoint is permitAll; wrong creds go through to the controller
        // and throw IllegalArgumentException which the GlobalExceptionHandler maps to 401
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nobody@test.com","password":"wrong"}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `register endpoint creates user without authentication`() {
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"SecTest","email":"sectest${System.currentTimeMillis()}@test.com","password":"testpass123"}"""
        }.andExpect {
            status { isCreated() }
        }
    }

    // ── Protected endpoints — require authentication ───────────────────────────

    @Test
    fun `GET expenses without token returns 401`() {
        mockMvc.get("/api/v1/expenses")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET reports summary without token returns 401`() {
        mockMvc.get("/api/v1/reports/summary")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET budgets without token returns 401`() {
        mockMvc.get("/api/v1/budgets")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET notifications without token returns 401`() {
        mockMvc.get("/api/v1/notifications")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET export CSV without token returns 401`() {
        mockMvc.get("/api/v1/reports/export?format=csv")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `GET export PDF without token returns 401`() {
        mockMvc.get("/api/v1/reports/export?format=pdf")
            .andExpect { status { isUnauthorized() } }
    }
}
