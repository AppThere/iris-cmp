package org.appthere.iris.buildlogic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Kotlin rule table is a copy of docs/architecture.md section 1; this keeps the two in step. */
class ArchitectureTableSyncTest {
    private val rows: List<List<String>> =
        File(requireNotNull(System.getProperty("iris.architectureDoc")) { "iris.architectureDoc is not set" })
            .readLines()
            .dropWhile { !it.startsWith("| Module |") }
            .drop(2)
            .takeWhile { it.startsWith("|") }
            .map { line -> line.trim('|').split("|").map { it.trim() } }

    @Test
    fun `allowed dependencies match the document`() {
        val fromDoc = rows.associate { it[0].trim('`') to dependencyCell(it[2]) }
        val fromCode = ArchitectureRules.table.mapValues { (_, rule) -> if (rule.anyDependency) null else rule.allowed }

        assertEquals(fromDoc, fromCode)
    }

    @Test
    fun `imports are checked in the modules the document keeps free of platform APIs`() {
        // iris-pixels is an engine module; its "TileBuffer actuals only" exemption is added with S8.
        val fromDoc = rows.filter { it[3] == "no" || it[0] == "`iris-pixels`" }.map { it[0].trim('`') }.toSet()
        val fromCode = ArchitectureRules.table.filterValues { it.importsChecked }.keys

        assertEquals(fromDoc, fromCode)
    }

    /** `none` is no dependency, `all` and `any (test only)` are any (null), otherwise short names such as `core, pixels`. */
    private fun dependencyCell(cell: String): Set<String>? =
        when {
            cell == "none" -> emptySet()
            cell == "all" || cell.startsWith("any") -> null
            else -> cell.split(",").map { "iris-" + it.trim() }.toSet()
        }
}
