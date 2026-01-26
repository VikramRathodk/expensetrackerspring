package com.devvikram.expensetracker.expensetracker.config

import io.github.cdimascio.dotenv.dotenv
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class EnvConfig {

    @PostConstruct
    fun loadEnv() {
        val dotenv = dotenv {
            ignoreIfMissing = true
        }
        System.setProperty("JWT_SECRET", dotenv["JWT_SECRET"])
    }
}