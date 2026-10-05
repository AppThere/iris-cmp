package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `composable functions are compiled by the compose compiler for jvm and android`() {
        val project =
            FixtureProject.kmpCompose(dir).withCommonSource(
                "Hello.kt",
                """
                import androidx.compose.runtime.Composable

                @Composable
                public fun Hello() {}
                """.trimIndent(),
            )
        val module = ":${FixtureProject.MODULE}"

        project.runner("$module:compileKotlinJvm", "$module:compileAndroidMain").build()

        // The compose compiler adds a Composer parameter to every @Composable function.
        val classes = project.moduleDir.resolve("build").walk().filter { it.name == "HelloKt.class" }.toList()
        assertTrue(classes.size >= 2, "Expected HelloKt.class for jvm and android, found $classes")
        classes.forEach { assertTrue(COMPOSER in it.readBytes().decodeToString(), "$it was not compose-compiled") }
    }

    @Test
    fun `compose ui tests run on the desktop jvm`() {
        val project =
            FixtureProject
                .kmpCompose(dir)
                .withCommonSource(
                    "Hello.kt",
                    """
                    import androidx.compose.runtime.Composable

                    @Composable
                    public fun Hello() {}
                    """.trimIndent(),
                ).withSource(
                    "jvmTest",
                    "HelloTest.kt",
                    """
                    import androidx.compose.ui.test.ExperimentalTestApi
                    import androidx.compose.ui.test.runComposeUiTest
                    import kotlin.test.Test
import kotlin.test.assertEquals

                    class HelloTest {
                        @OptIn(ExperimentalTestApi::class)
                        @Test
                        fun composes() = runComposeUiTest { setContent { Hello() } }
                    }
                    """.trimIndent(),
                )
        val jvmTest = ":${FixtureProject.MODULE}:jvmTest"

        val result = project.runner(jvmTest).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(jvmTest)?.outcome)
        assertTrue(project.moduleDir.resolve("build/test-results/jvmTest/TEST-HelloTest.xml").exists())
    }

    private companion object {
        const val COMPOSER = "Landroidx/compose/runtime/Composer;"
    }
}
