package com.devvikram.expensetracker.expensetracker.config

import  io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class EnvConfig : EnvironmentPostProcessor {

    companion object {
        private val REQUIRED_VARS = listOf(
            "JWT_SECRET",
            "DATABASE_URL",
            "PGUSER",
            "PGPASSWORD",
            "PORT"
        )
    }

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication
    ) {
        // Skip env-var enforcement in the test profile — test properties supply all values directly
        if (environment.activeProfiles.any { it == "test" }) return

        val dotenv = dotenv {
            ignoreIfMissing = true
        }

        val props = mutableMapOf<String, Any>()
        val missing = mutableListOf<String>()

        REQUIRED_VARS.forEach { key ->
            
            val osValue = System.getenv(key) ?: ""
            val value = dotenv.get(key, osValue)
            if (value.isBlank()) {
                missing.add(key)
            } else {
                props[key] = value
            }
        }

        if (missing.isNotEmpty()) {
            throw IllegalStateException(
                "Missing required environment variables: ${missing.joinToString(", ")}. " +
                "Set them in your .env file or as OS environment variables."
            )
        }

        environment.propertySources.addFirst(
            MapPropertySource("dotenvProperties", props)
        )
    }
}
