package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
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

    private companion object {
        const val COMPOSER = "Landroidx/compose/runtime/Composer;"
    }
}
