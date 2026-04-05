plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.devvikram.expensetracker"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
	enabled = true
}

tasks.withType<Jar> {
	enabled = true
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")

	// Flyway — database migrations (starter brings the SB4 auto-configuration)
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

	// Spring Security
	implementation("org.springframework.boot:spring-boot-starter-security")


	implementation("org.springframework.boot:spring-boot-starter-validation")



	implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")


	// Swagger / OpenAPI — Spring Boot 4.0 / Spring 7 compatible
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")

	// CSV export
	implementation("org.apache.commons:commons-csv:1.12.0")

	// PDF export (OpenPDF — actively maintained iText fork, Apache 2.0)
	implementation("com.github.librepdf:openpdf:1.3.30")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Mockito for Kotlin
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testImplementation("org.mockito:mockito-core:5.14.2")

	// H2 in-memory DB for integration tests
	testRuntimeOnly("com.h2database:h2")

	// Spring Security test support
	testImplementation("org.springframework.security:spring-security-test")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// ── Postman Sync Checker ──────────────────────────────────────────────────────
// Scans controller .kt files for HTTP endpoints and compares against
// the Postman complete collection JSON.  Reports any undocumented endpoints.
//
// Run:         ./gradlew checkPostmanSync
// Verbose:     ./gradlew checkPostmanSync -Dpostman.verbose=true
// ─────────────────────────────────────────────────────────────────────────────
tasks.register<JavaExec>("checkPostmanSync") {
    group = "verification"
    description = "Check that all controller endpoints are documented in the Postman collection"

    // Use the Kotlin compiler's script runner that ships with the Kotlin stdlib on the classpath
    classpath = configurations.getByName("kotlinCompilerClasspath").asFileTree
        .filter { it.name.startsWith("kotlin-main-kts") || it.name.startsWith("kotlin-scripting") }
        .let { files(it) }
        .let { scriptClasspath ->
            // Fallback: just use the Kotlin stdlib jar as the runner bootstrap
            configurations.getByName("compileClasspath")
        }

    // Instead of running the .kts directly (requires kotlinc on PATH),
    // we inline the logic as a plain Gradle task written in Kotlin DSL.
    doFirst { }   // placeholder — actual logic is below in doLast
    mainClass.set("") // not used

    // Override the whole task action with pure Gradle/Kotlin code
    // (avoids needing kotlinc-jvm on PATH)
    actions.clear()
    doLast {
        val httpAnnotations = mapOf(
            "@GetMapping"    to "GET",
            "@PostMapping"   to "POST",
            "@PutMapping"    to "PUT",
            "@PatchMapping"  to "PATCH",
            "@DeleteMapping" to "DELETE"
        )

        data class Ep(val method: String, val path: String, val source: String)

        fun basePath(text: String): String =
            Regex("""@RequestMapping\(\s*["']([^"']+)["']\s*\)""")
                .find(text)?.groupValues?.get(1) ?: ""

        fun endpoints(text: String, base: String, src: String): List<Ep> {
            val eps = mutableListOf<Ep>()
            val lines = text.lines()
            for ((i, line) in lines.withIndex()) {
                for ((ann, method) in httpAnnotations) {
                    if (!line.trimStart().startsWith(ann)) continue
                    val block = lines.drop(i).take(5).joinToString(" ")
                    val sub = Regex("""$ann\s*\(\s*(?:value\s*=\s*)?["']([^"']*?)["']""")
                        .find(block)?.groupValues?.get(1) ?: ""
                    val full = (base.trimEnd('/') + "/" + sub.trimStart('/')).trimEnd('/')
                    eps += Ep(method, full, src)
                    break
                }
            }
            return eps
        }

        val ctrlDir = file(
            "src/main/kotlin/com/devvikram/expensetracker/expensetracker/controllers"
        )
        val ctrlEps = ctrlDir.walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { f ->
                val txt = f.readText()
                endpoints(txt, basePath(txt), f.name)
            }.toList()

        val postmanFile = file(
            "src/main/kotlin/com/devvikram/expensetracker/expensetracker/flow/" +
            "ExpenseTestings/postman_collections/expense_tracker_complete_collection.json"
        )

        println("\n╔══════════════════════════════════════════════════════════════╗")
        println("║        POSTMAN SYNC CHECKER — Expense Tracker               ║")
        println("╚══════════════════════════════════════════════════════════════╝\n")

        if (!postmanFile.exists()) {
            println("⚠️  Postman collection not found at:")
            println("   ${postmanFile.absolutePath}\n")
            return@doLast
        }

        val json = postmanFile.readText()
        val pmEps = Regex(""""method"\s*:\s*"([A-Z]+)"[\s\S]*?"raw"\s*:\s*"[^"]*?(/api[^"]*?)"""")
            .findAll(json)
            .map { m ->
                val method = m.groupValues[1]
                var p = m.groupValues[2].substringBefore("?")
                    // {{base_url}} → strip; {{expense_id}} etc → treat as path param {id}
                    .replace(Regex("\\{\\{base_url\\}\\}"), "")
                    .replace(Regex("\\{\\{[^}]+\\}\\}"), "{id}")
                    // :paramName style → {id}
                    .replace(Regex(":[a-zA-Z_]+"), "{id}")
                    .trim()
                if (!p.startsWith("/")) p = "/$p"
                method to p
            }.toList().distinct()

        println("📋 Controller endpoints : ${ctrlEps.size}")
        println("📬 Postman endpoints    : ${pmEps.size}\n")

        fun norm(p: String) = p.replace(Regex("\\{[^}]+\\}"), "{?}")

        val missing = ctrlEps.filter { ep ->
            pmEps.none { (pm, pp) -> pm == ep.method && norm(pp) == norm(ep.path) }
        }

        if (missing.isEmpty()) {
            println("✅ All controller endpoints are documented in the Postman collection!")
        } else {
            println("❌ ${missing.size} endpoint(s) MISSING from the Postman collection:\n")
            missing.sortedWith(compareBy({ it.source }, { it.method }, { it.path }))
                .forEach { ep -> println("   [${ep.method.padEnd(6)}] ${ep.path}  (${ep.source})") }
            println()
            println("👉 Update expense_tracker_complete_collection.json, then re-run:")
            println("   ./gradlew checkPostmanSync\n")
            throw GradleException("Postman collection is out of sync. See output above.")
        }

        if (project.hasProperty("postman.verbose")) {
            println("\n── All Postman endpoints ─────────────────────────────────────────")
            pmEps.sortedWith(compareBy({ it.first }, { it.second }))
                .forEach { (m, p) -> println("   [${m.padEnd(6)}] $p") }
            println("\n── All controller endpoints ──────────────────────────────────────")
            ctrlEps.sortedWith(compareBy({ it.method }, { it.path }))
                .forEach { ep -> println("   [${ep.method.padEnd(6)}] ${ep.path}  (${ep.source})") }
        }
    }
}
