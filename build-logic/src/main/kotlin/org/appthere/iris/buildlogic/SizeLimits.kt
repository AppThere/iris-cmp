package org.appthere.iris.buildlogic

import java.io.File

private const val SOURCE_LIMIT = 400
private const val TEST_LIMIT = 600
private val EXEMPT_SEGMENTS = setOf("testdata", "generated")

/**
 * Line limit for a Kotlin file at [relativePath] (`src/<sourceSet>/...`) under docs/coding-standards.md section 2,
 * or null when exempt (`testdata/` fixtures and generated code).
 */
internal fun fileLengthLimit(relativePath: String): Int? {
    val segments = relativePath.split('/')
    if (segments.any { it in EXEMPT_SEGMENTS }) return null
    val sourceSet = segments.getOrElse(1) { "" }
    return if (sourceSet.startsWith("test") || sourceSet.contains("Test")) TEST_LIMIT else SOURCE_LIMIT
}

/** Every Kotlin file under [moduleDir]`/src` that is longer than its limit, in path order. */
internal fun findOversizedFiles(moduleDir: File): List<String> =
    moduleDir
        .resolve("src")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .map { it.relativeTo(moduleDir).invariantSeparatorsPath to it }
        .sortedBy { it.first }
        .mapNotNull { (path, file) ->
            val limit = fileLengthLimit(path) ?: return@mapNotNull null
            val lines = file.useLines { it.count() }
            if (lines > limit) "$path has $lines lines (limit $limit)" else null
        }.toList()
