package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class QualityConventionTest {
    @TempDir
    lateinit var dir: File

    private val module = ":${FixtureProject.MODULE}"

    @Test
    fun `badly formatted kotlin fails spotlessCheck`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Answer.kt", "public fun answer( ):Int=42\n")

        val result = project.runner("$module:spotlessCheck").buildAndFail()

        assertContains(result.output, "spotlessApply")
    }

    @Test
    fun `detekt reports a code smell`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Nothing.kt", "public fun nothing() {}\n")

        val result = project.runner("$module:detekt").buildAndFail()

        assertContains(result.output, "EmptyFunctionBlock")
    }

    @Test
    fun `check runs formatting, static analysis, size limits and coverage verification`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Answer.kt", "public fun answer(): Int = 42\n")

        val result = project.runner("$module:check", "--dry-run").build()

        assertContains(result.output, "$module:spotlessCheck SKIPPED")
        assertContains(result.output, "$module:detekt SKIPPED")
        // Rules that need type resolution (such as LongParameterList) only run in the per-compilation tasks.
        assertContains(result.output, "$module:detektMainJvm SKIPPED")
        assertContains(result.output, "$module:detektTestJvm SKIPPED")
        assertContains(result.output, "$module:koverVerify SKIPPED")
        assertContains(result.output, "$module:verifySizeLimits SKIPPED")
    }

    @Test
    fun `a 401-line source file fails verifySizeLimits`() {
        val code = "public fun answer(): Int = 42\n" + "// filler\n".repeat(400)
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Long.kt", code)

        val result = project.runner("$module:verifySizeLimits").buildAndFail()

        assertContains(result.output, "src/commonMain/kotlin/Long.kt has 401 lines (limit 400)")
    }
}
