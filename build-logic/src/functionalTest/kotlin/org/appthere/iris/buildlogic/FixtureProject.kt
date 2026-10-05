package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.GradleRunner
import java.io.File

/** A throwaway Gradle build in [dir] with one module using an Iris convention plugin, for TestKit tests. */
internal class FixtureProject(
    private val dir: File,
) {
    fun withCommonSource(
        fileName: String,
        code: String,
    ): FixtureProject = withSource("commonMain", fileName, code)

    /** Appends [script] to the fixture module's `build.gradle.kts`. */
    fun withBuildScript(script: String): FixtureProject {
        dir.resolve("$MODULE/build.gradle.kts").appendText(script.trimIndent() + "\n")
        return this
    }

    fun withSource(
        sourceSet: String,
        fileName: String,
        code: String,
    ): FixtureProject {
        val file = dir.resolve("$MODULE/src/$sourceSet/kotlin/$fileName")
        file.parentFile.mkdirs()
        file.writeText(code)
        return this
    }

    /** Writes [content] to [path], relative to the fixture module. */
    fun withFile(
        path: String,
        content: String,
    ): FixtureProject {
        val file = dir.resolve("$MODULE/$path")
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent() + "\n")
        return this
    }

    /** Directory of the fixture module, for assertions on build outputs. */
    val moduleDir: File get() = dir.resolve(MODULE)

    fun runner(vararg tasks: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withTestKitDir(File(requiredProperty("iris.testKitDir")))
            .withArguments(*tasks, "--stacktrace")
            .forwardOutput()

    companion object {
        const val MODULE = "iris-fixture"

        fun kmpLibrary(dir: File): FixtureProject = withPlugin(dir, "iris.kmp.library")

        fun kmpCompose(dir: File): FixtureProject = withPlugin(dir, "iris.kmp.compose")

        fun desktopApp(dir: File): FixtureProject = withPlugin(dir, "iris.app.desktop")

        fun androidApp(dir: File): FixtureProject = withPlugin(dir, "iris.app.android")

        fun iosApp(dir: File): FixtureProject = withPlugin(dir, "iris.app.ios")

        fun jvmApp(dir: File): FixtureProject = withPlugin(dir, "iris.jvm.app")

        private fun withPlugin(
            dir: File,
            pluginId: String,
        ): FixtureProject {
            val catalog = File(requiredProperty("iris.versionCatalog")).invariantSeparatorsPath
            dir.resolve("settings.gradle.kts").writeText(
                """
                dependencyResolutionManagement {
                    repositories {
                        google()
                        mavenCentral()
                    }
                    versionCatalogs {
                        create("libs") { from(files("$catalog")) }
                    }
                }
                rootProject.name = "fixture"
                include(":$MODULE")
                """.trimIndent(),
            )
            dir.resolve("gradle.properties").writeText("org.gradle.jvmargs=-Xmx2g\n")
            val module = dir.resolve(MODULE)
            module.mkdirs()
            module.resolve("build.gradle.kts").writeText("plugins { id(\"$pluginId\") }\n")
            return FixtureProject(dir)
        }

        private fun requiredProperty(name: String): String =
            requireNotNull(System.getProperty(name)) { "System property $name is not set by the functionalTest task" }
    }
}
