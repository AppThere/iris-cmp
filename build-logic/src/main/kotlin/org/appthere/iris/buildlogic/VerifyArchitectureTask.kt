package org.appthere.iris.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Checks every module's project dependencies and Kotlin imports against [ArchitectureRules] (D-019). */
abstract class VerifyArchitectureTask : DefaultTask() {
    /** Module name to the project dependencies declared in its main (non-test) configurations. */
    @get:Input
    abstract val mainDependencies: MapProperty<String, List<String>>

    /** Module name to the project dependencies declared in its test configurations. */
    @get:Input
    abstract val testDependencies: MapProperty<String, List<String>>

    /** Module name to its project directory, to read and attribute the [sources]. */
    @get:Input
    abstract val moduleDirectories: MapProperty<String, String>

    /** Every module's Kotlin sources under `src/`; declared so that the task reruns when an import changes. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val main = mainDependencies.get()
        val test = testDependencies.get()
        val facts =
            moduleDirectories.get().map { (name, path) ->
                ModuleFacts(
                    name = name,
                    mainDependencies = main[name].orEmpty().toSet() - name,
                    testDependencies = test[name].orEmpty().toSet() - name,
                    imports = importsOf(File(path)),
                )
            }
        val violations = findArchitectureViolations(facts)
        report.get().asFile.writeText(violations.joinToString("\n", postfix = "\n"))
        if (violations.isNotEmpty()) {
            throw GradleException("Architecture violations (docs/architecture.md section 1):\n" + violations.joinToString("\n") { "  - $it" })
        }
    }

    private fun importsOf(moduleDir: File): List<SourceImport> =
        moduleDir
            .resolve("src")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .flatMap { parseImports(it.relativeTo(moduleDir).invariantSeparatorsPath, it.readText()) }
            .toList()
}
