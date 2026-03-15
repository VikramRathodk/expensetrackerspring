package com.devvikram.expensetracker.expensetracker.config

import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Explicit Flyway configuration.
 *
 * Spring Boot 4.0 has modularised its auto-configuration: FlywayAutoConfiguration
 * no longer auto-triggers reliably from spring-boot-autoconfigure alone.
 * Declaring the bean here guarantees:
 *   1. Flyway always runs (no auto-config surprises).
 *   2. Spring Boot JPA auto-config sees the bean named "flyway" and waits for it
 *      before building the EntityManagerFactory (@DependsOn("flyway") in SB's JPA config).
 *
 * Self-healing: if flyway_schema_history exists but is stuck at baseline version 0
 * (no real migrations applied yet), it is dropped so Flyway re-baselines at version 12.
 */
@Configuration
class FlywayConfig(private val dataSource: DataSource) {

    @Bean
    fun flyway(): Flyway {
        resetStuckHistoryTable()

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("12")   // tables V1-V12 already exist; only V13 needs to run
            .load()
        flyway.repair()
        flyway.migrate()
        return flyway
    }

    /**
     * If flyway_schema_history exists but contains only the baseline-0 row
     * (no actual migration rows with version > 0 were successfully applied),
     * drop the table so a fresh baseline-12 can be created on the next migrate().
     */
    private fun resetStuckHistoryTable() {
        dataSource.connection.use { conn ->
            // Check whether the history table exists at all
            val tableExists = conn.metaData
                .getTables(null, null, "flyway_schema_history", arrayOf("TABLE"))
                .use { it.next() }

            if (!tableExists) return

            // Count successfully applied migrations with version > 0
            val realMigrationsApplied = conn
                .prepareStatement(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true AND version <> '0'"
                )
                .use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next(); rs.getInt(1)
                    }
                }

            if (realMigrationsApplied == 0) {
                // Table is stuck at baseline-0; drop it so we can re-baseline at 12
                conn.createStatement().use { it.execute("DROP TABLE flyway_schema_history") }
            }
        }
    }
}
