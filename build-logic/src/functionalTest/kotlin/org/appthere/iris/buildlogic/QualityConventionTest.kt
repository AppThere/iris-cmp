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
    fun `check runs formatting, static analysis and coverage verification`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Answer.kt", "public fun answer(): Int = 42\n")

        val result = project.runner("$module:check", "--dry-run").build()

        assertContains(result.output, "$module:spotlessCheck SKIPPED")
        assertContains(result.output, "$module:detekt SKIPPED")
        assertContains(result.output, "$module:koverVerify SKIPPED")
    }
}
