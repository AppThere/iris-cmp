package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/** Applied to the root project: registers `verifyArchitecture` over all subprojects and runs it from `check` (D-019). */
class ArchitectureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("lifecycle-base")
        val modules = target.subprojects
        val verify =
            target.tasks.register<VerifyArchitectureTask>("verifyArchitecture") {
                group = "verification"
                description = "Checks module dependencies and imports against docs/architecture.md section 1."
                // Providers run after every module is configured, so declared dependencies are complete.
                mainDependencies.set(target.provider { modules.associate { it.name to projectDependencies(it, tests = false) } })
                testDependencies.set(target.provider { modules.associate { it.name to projectDependencies(it, tests = true) } })
                moduleDirectories.set(modules.associate { it.name to it.projectDir.absolutePath })
                sources.from(modules.map { module -> module.fileTree("src") { include("**/*.kt") } })
                report.set(target.layout.buildDirectory.file("reports/architecture/violations.txt"))
            }
        target.tasks.named("check") { dependsOn(verify) }
    }

    private fun projectDependencies(
        module: Project,
        tests: Boolean,
    ): List<String> =
        module.configurations
            .filter { isTestConfiguration(it.name) == tests }
            .flatMap { it.dependencies.withType<ProjectDependency>() }
            .map { it.path.removePrefix(":") }
            .distinct()
            .sorted()

    /** `testImplementation`, `commonTestImplementation`, `androidHostTestImplementation`, `jvmTestCompileOnly` and the like. */
    private fun isTestConfiguration(name: String): Boolean = name.startsWith("test") || name.contains("Test")
}
