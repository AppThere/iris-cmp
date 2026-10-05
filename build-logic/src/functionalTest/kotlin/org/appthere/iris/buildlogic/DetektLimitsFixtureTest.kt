package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** docs/coding-standards.md section 2, as enforced by detekt: each limit passes at the limit and fails one past it. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DetektLimitsFixtureTest {
    private lateinit var result: BuildResult

    @BeforeAll
    fun runDetekt(
        @TempDir dir: File,
    ) {
        val project = FixtureProject.kmpLibrary(dir)
        CASES.forEach { (file, code) -> project.withCommonSource(file, code) }
        // detektMainJvm adds type resolution, which LongParameterList and other rules need.
        val module = ":${FixtureProject.MODULE}"
        result = project.runner("$module:detekt", "$module:detektMainJvm", "--continue").buildAndFail()
    }

    @Test
    fun `a function may have 50 lines but not 51`() = assertLimit("LongMethod", "Length")

    @Test
    fun `cyclomatic complexity may be 12 but not 13`() = assertLimit("CyclomaticComplexMethod", "Complexity")

    @Test
    fun `nesting depth may be 4 but not 5`() = assertLimit("NestedBlockDepth", "Nesting")

    @Test
    fun `a function may have 7 parameters but not 8`() = assertLimit("LongParameterList", "Parameters")

    @Test
    fun `a class may have 20 public functions but not 21`() = assertLimit("TooManyFunctions", "Functions")

    private fun assertLimit(
        rule: String,
        case: String,
    ) {
        val findings = result.output.lines().filter { "[$rule]" in it }
        assertTrue(findings.any { "${case}Over.kt" in it }, "Expected $rule in ${case}Over.kt:\n${result.output}")
        assertEquals(emptyList(), result.output.lines().filter { "${case}AtLimit.kt" in it }, "${case}AtLimit.kt must be clean")
    }

    private companion object {
        val CASES: Map<String, String> =
            mapOf(
                "LengthAtLimit.kt" to longFunction("lengthAtLimit", 50),
                "LengthOver.kt" to longFunction("lengthOver", 51),
                "ComplexityAtLimit.kt" to branchyFunction("complexityAtLimit", 12),
                "ComplexityOver.kt" to branchyFunction("complexityOver", 13),
                "NestingAtLimit.kt" to nestedFunction("nestingAtLimit", 4),
                "NestingOver.kt" to nestedFunction("nestingOver", 5),
                "ParametersAtLimit.kt" to parameterFunction("parametersAtLimit", 7),
                "ParametersOver.kt" to parameterFunction("parametersOver", 8),
                "FunctionsAtLimit.kt" to classWithFunctions("FunctionsAtLimit", 20),
                "FunctionsOver.kt" to classWithFunctions("FunctionsOver", 21),
            )

        /** A function spanning [lines] lines, signature and closing brace included. */
        fun longFunction(
            name: String,
            lines: Int,
        ): String =
            "public fun $name() {\n" + (1..lines - 2).joinToString("") { "    println(\"line\")\n" } + "}\n"

        /** Complexity is 1 plus one per `if`. */
        fun branchyFunction(
            name: String,
            complexity: Int,
        ): String =
            "public fun $name(s: String) {\n" +
                (1 until complexity).joinToString("") { "    if (s == \"v$it\") println(\"v$it\")\n" } + "}\n"

        /** [depth] nested `if` blocks inside the function body. */
        fun nestedFunction(
            name: String,
            depth: Int,
        ): String {
            val open = (1..depth).joinToString("") { "    ".repeat(it) + "if (a) {\n" }
            val close = (depth downTo 1).joinToString("") { "    ".repeat(it) + "}\n" }
            return "public fun $name(a: Boolean) {\n" + open + "    ".repeat(depth + 1) + "println(\"deep\")\n" + close + "}\n"
        }

        fun parameterFunction(
            name: String,
            count: Int,
        ): String {
            val names = (1..count).map { "p$it" }
            return "public fun $name(${names.joinToString { "$it: String" }}) {\n" +
                "    println(${names.joinToString(" + ")})\n}\n"
        }

        fun classWithFunctions(
            name: String,
            count: Int,
        ): String =
            "public class $name {\n" + (1..count).joinToString("") { "    public fun f$it() {\n        println(\"f$it\")\n    }\n" } + "}\n"
    }
}
