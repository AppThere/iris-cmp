package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ExplicitApiConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `public declaration without a visibility modifier fails to compile`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Greeting.kt", "fun greeting(): String = \"hi\"\n")

        val result = project.runner(":${FixtureProject.MODULE}:compileKotlinJvm").buildAndFail()

        assertContains(result.output, "Visibility must be specified in explicit API mode")
    }

    @Test
    fun `public declaration with a visibility modifier compiles`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Greeting.kt", "public fun greeting(): String = \"hi\"\n")

        val result = project.runner(":${FixtureProject.MODULE}:compileKotlinJvm").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":${FixtureProject.MODULE}:compileKotlinJvm")?.outcome)
    }
}
