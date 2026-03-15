#!/usr/bin/env kotlin
/**
 * check_postman_sync.kts
 *
 * Scans all controller .kt files for HTTP mapping annotations,
 * reads the Postman complete collection JSON, and reports any
 * controller endpoints NOT documented in the collection.
 *
 * Usage (via Gradle task):  ./gradlew checkPostmanSync
 * Usage (standalone):       kotlinc-jvm -script scripts/check_postman_sync.kts
 */

import java.io.File

// ── Config ────────────────────────────────────────────────────────────────────

val projectRoot = File(System.getProperty("project.root", "."))

val controllersDir = projectRoot.resolve(
    "src/main/kotlin/com/devvikram/expensetracker/expensetracker/controllers"
)

val postmanFile = projectRoot.resolve(
    "src/main/kotlin/com/devvikram/expensetracker/expensetracker/flow/" +
    "ExpenseTestings/postman_collections/expense_tracker_complete_collection.json"
)

// ── Step 1: Extract endpoints from controller files ───────────────────────────

data class Endpoint(val method: String, val path: String, val source: String)

val methodAnnotations = mapOf(
    "@GetMapping"    to "GET",
    "@PostMapping"   to "POST",
    "@PutMapping"    to "PUT",
    "@PatchMapping"  to "PATCH",
    "@DeleteMapping" to "DELETE"
)

fun extractBasePath(content: String): String {
    val regex = Regex("""@RequestMapping\(\s*["']([^"']+)["']\s*\)""")
    return regex.find(content)?.groupValues?.get(1) ?: ""
}

fun extractMappings(content: String, basePath: String, fileName: String): List<Endpoint> {
    val results = mutableListOf<Endpoint>()
    val lines = content.lines()

    for ((index, line) in lines.withIndex()) {
        for ((annotation, method) in methodAnnotations) {
            if (!line.trimStart().startsWith(annotation)) continue

            // Extract path from annotation - handle multi-line, value=, quotes
            val annotationBlock = lines.drop(index).take(5).joinToString(" ")
            val pathRegex = Regex("""$annotation\s*\(\s*(?:value\s*=\s*)?["'](.*?)["']""")
            val noArgRegex = Regex("""$annotation\s*\(\s*\)""")
            val noParenRegex = Regex("""$annotation\s*$""")

            val subPath = when {
                pathRegex.containsMatchIn(annotationBlock) ->
                    pathRegex.find(annotationBlock)!!.groupValues[1]
                noArgRegex.containsMatchIn(annotationBlock) || noParenRegex.containsMatchIn(annotationBlock) ->
                    ""
                // annotation with just value (no quotes captured above) - try alternate
                else -> {
                    val altRegex = Regex("""$annotation\("([^"]+)"\)""")
                    altRegex.find(annotationBlock)?.groupValues?.get(1) ?: ""
                }
            }

            val fullPath = (basePath.trimEnd('/') + "/" + subPath.trimStart('/')).trimEnd('/')
            results.add(Endpoint(method, fullPath, fileName))
            break
        }
    }
    return results
}

val controllerEndpoints = mutableListOf<Endpoint>()

controllersDir.walkTopDown()
    .filter { it.extension == "kt" }
    .forEach { file ->
        val content = file.readText()
        val basePath = extractBasePath(content)
        controllerEndpoints += extractMappings(content, basePath, file.name)
    }

// ── Step 2: Extract endpoints from Postman collection ────────────────────────

fun extractPostmanEndpoints(json: String): List<Pair<String, String>> {
    val results = mutableListOf<Pair<String, String>>()

    // Match "method":"GET" ... "raw":"{{base_url}}/api/..." patterns
    val requestBlockRegex = Regex(
        """"method"\s*:\s*"([A-Z]+)"[\s\S]*?"raw"\s*:\s*"[^"]*?(/api[^"]*?)"""
    )

    requestBlockRegex.findAll(json).forEach { match ->
        val method = match.groupValues[1]
        var rawPath = match.groupValues[2]

        // Strip query params and normalize path params like :id -> {id}
        rawPath = rawPath.substringBefore("?")
        rawPath = rawPath.replace(Regex(":[a-zA-Z_]+")) { "{${it.value.removePrefix(":")}}" }

        // Strip {{base_url}} or http://... prefix, keep /api/...
        rawPath = rawPath.replace(Regex("\\{\\{[^}]+\\}\\}"), "").trimStart('/')
        if (!rawPath.startsWith("/")) rawPath = "/$rawPath"

        results.add(Pair(method, rawPath))
    }

    return results.distinct()
}

if (!postmanFile.exists()) {
    println("⚠️  Postman collection not found at:\n   ${postmanFile.absolutePath}")
    println("   Run the app, export the collection, and place it there.")
} else {
    val postmanJson = postmanFile.readText()
    val postmanEndpoints = extractPostmanEndpoints(postmanJson)

    // ── Step 3: Diff ──────────────────────────────────────────────────────────

    println("\n╔══════════════════════════════════════════════════════════════╗")
    println("║          POSTMAN SYNC CHECKER — Expense Tracker              ║")
    println("╚══════════════════════════════════════════════════════════════╝\n")

    println("📋 Controller endpoints found : ${controllerEndpoints.size}")
    println("📬 Postman endpoints found    : ${postmanEndpoints.size}\n")

    val missingInPostman = controllerEndpoints.filter { endpoint ->
        postmanEndpoints.none { (pm, pp) ->
            pm == endpoint.method &&
            // Normalize path params {id} / :id / {budgetId} all match
            pp.replace(Regex("\\{[^}]+\\}"), "{?}") ==
            endpoint.path.replace(Regex("\\{[^}]+\\}"), "{?}")
        }
    }

    if (missingInPostman.isEmpty()) {
        println("✅ All controller endpoints are documented in the Postman collection!")
    } else {
        println("❌ ${missingInPostman.size} endpoint(s) MISSING from Postman collection:\n")
        missingInPostman
            .sortedWith(compareBy({ it.source }, { it.method }, { it.path }))
            .forEach { ep ->
                println("   [${ep.method.padEnd(6)}] ${ep.path}  (${ep.source})")
            }
        println()
        println("👉 Update expense_tracker_complete_collection.json to include these endpoints.")
        println("   Then re-run: ./gradlew checkPostmanSync\n")
        // Exit with code 1 so the git hook blocks the commit
        Runtime.getRuntime().halt(1)
    }

    // ── Step 4: Show all documented endpoints (verbose) ───────────────────────
    val verbose = System.getProperty("postman.verbose", "false") == "true"
    if (verbose) {
        println("\n── Documented Postman endpoints ─────────────────────────────────")
        postmanEndpoints.sortedWith(compareBy({ it.first }, { it.second })).forEach { (m, p) ->
            println("   [${m.padEnd(6)}] $p")
        }
        println("\n── Controller endpoints ──────────────────────────────────────────")
        controllerEndpoints.sortedWith(compareBy({ it.method }, { it.path })).forEach { ep ->
            println("   [${ep.method.padEnd(6)}] ${ep.path}  (${ep.source})")
        }
    }
}
