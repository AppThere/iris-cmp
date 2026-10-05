package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class DesktopAppConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `desktop app runs its main class with compose desktop on the classpath`() {
        val project =
            FixtureProject.desktopApp(dir).withSource(
                "main",
                "org/appthere/iris/fixture/Main.kt",
                """
                package org.appthere.iris.fixture

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.window.WindowPosition

                @Composable
                fun Screen() {}

                fun main() {
                    println("POSITION=" + WindowPosition.PlatformDefault)
                }
                """.trimIndent(),
            )

        val result = project.runner(":${FixtureProject.MODULE}:run").build()

        assertContains(result.output, "POSITION=PlatformDefault")
        val screen = project.moduleDir.resolve("build/classes/kotlin/main/org/appthere/iris/fixture/MainKt.class")
        assertTrue(COMPOSER in screen.readBytes().decodeToString(), "MainKt was not compose-compiled")
    }

    @Test
    fun `the app jvm may use native access, which Skia needs on JDK 24 and later`() {
        val project =
            FixtureProject.desktopApp(dir).withBuildScript(
                """
                tasks.register("printJvmArgs") {
                    val args = compose.desktop.application.jvmArgs.joinToString(" ")
                    doLast { println("JVM_ARGS=" + args) }
                }
                """,
            )

        val result = project.runner(":${FixtureProject.MODULE}:printJvmArgs").build()

        assertContains(result.output, "--enable-native-access=ALL-UNNAMED")
    }

    private companion object {
        const val COMPOSER = "Landroidx/compose/runtime/Composer;"
    }
}
