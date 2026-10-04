package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonTestConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `common tests run on the jvm and the android host`() {
        val project =
            FixtureProject
                .kmpLibrary(dir)
                .withCommonSource("Answer.kt", "public fun answer(): Int = 42\n")
                .withSource(
                    "commonTest",
                    "AnswerTest.kt",
                    """
                    import kotlin.test.Test
                    import kotlin.test.assertEquals

                    class AnswerTest {
                        @Test
                        fun answerIs42() {
                            assertEquals(42, answer())
                        }
                    }
                    """.trimIndent(),
                )
        val jvmTest = ":${FixtureProject.MODULE}:jvmTest"
        val androidTest = ":${FixtureProject.MODULE}:testAndroidHostTest"

        val result = project.runner(jvmTest, androidTest).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(jvmTest)?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(androidTest)?.outcome)
        assertEquals(true, project.moduleDir.resolve("build/test-results/jvmTest/TEST-AnswerTest.xml").exists())
    }
}
