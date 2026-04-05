package com.devvikram.expensetracker.expensetracker.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
    info = Info(
        title = "Expense Tracker API",
        version = "1.0",
        description = """
            Spring Boot 4.0 Expense Tracker REST API

            **Authentication:** All endpoints (except /api/v1/auth/register, /api/v1/auth/login, /api/v1/auth/roles)
            require a Bearer JWT token. Use the Login endpoint to obtain a token, then click
            'Authorize' and enter: Bearer <your-token>
        """
    ),
    security = [SecurityRequirement(name = "bearerAuth")]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter your JWT token obtained from POST /api/auth/login"
)
@Configuration
class OpenApiConfig
