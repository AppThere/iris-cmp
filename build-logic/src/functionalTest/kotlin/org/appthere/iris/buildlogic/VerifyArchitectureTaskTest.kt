package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class VerifyArchitectureTaskTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `fails when iris-core depends on iris-model`() {
        modules(
            "iris-core" to "dependencies { implementation(project(\":iris-model\")) }",
            "iris-model" to "",
        )

        val result = runner("verifyArchitecture").buildAndFail()

        assertContains(result.output, "iris-core must not depend on iris-model (allowed: none)")
    }

    @Test
    fun `fails when an engine file imports java awt Color`() {
        modules("iris-core" to "")
        source("iris-core", "src/main/kotlin/Paint.kt", "import java.awt.Color\n\nval red: Color = Color.RED\n")

        val result = runner("verifyArchitecture").buildAndFail()

        assertContains(result.output, "iris-core must not import java.awt.Color (src/main/kotlin/Paint.kt:1)")
    }

    @Test
    fun `check runs verifyArchitecture, which passes on an allowed graph with the configuration cache`() {
        modules(
            "iris-core" to "",
            "iris-pixels" to "dependencies { implementation(project(\":iris-core\")) }",
            "iris-testing" to "",
            "iris-color" to
                """
                dependencies {
                    implementation(project(":iris-pixels"))
                    testImplementation(project(":iris-testing"))
                }
                """.trimIndent(),
        )

        val result = runner("check", "--configuration-cache").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyArchitecture")?.outcome)
    }

    private fun modules(vararg modules: Pair<String, String>) {
        val catalog = File(requiredProperty("iris.versionCatalog")).invariantSeparatorsPath
        val includes = modules.joinToString("\n") { "include(\":${it.first}\")" }
        dir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                repositories { mavenCentral() }
                versionCatalogs { create("libs") { from(files("$catalog")) } }
            }
            rootProject.name = "fixture"
            $includes
            """.trimIndent(),
        )
        dir.resolve("build.gradle.kts").writeText("plugins { id(\"iris.architecture\") }\n")
        modules.forEach { (name, script) ->
            dir.resolve(name).mkdirs()
            dir.resolve("$name/build.gradle.kts").writeText("plugins { `java-library` }\n\n$script\n")
        }
    }

    private fun source(
        module: String,
        path: String,
        code: String,
    ) {
        val file = dir.resolve("$module/$path")
        file.parentFile.mkdirs()
        file.writeText(code)
    }

    private fun runner(vararg tasks: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withTestKitDir(File(requiredProperty("iris.testKitDir")))
            .withArguments(*tasks, "--stacktrace")
            .forwardOutput()

    private fun requiredProperty(name: String): String =
        requireNotNull(System.getProperty(name)) { "System property $name is not set by the functionalTest task" }
}
