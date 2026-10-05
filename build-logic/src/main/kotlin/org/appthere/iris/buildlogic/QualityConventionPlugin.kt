package org.appthere.iris.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Formatting (Spotless with ktlint), static analysis (detekt with the Iris limits), file size limits,
 * the dependency license check and coverage (Kover), all wired into `check`.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        applySpotless(target)
        applyDetekt(target)
        applySizeLimits(target)
        applyLicenseCheck(target)
        target.pluginManager.apply("org.jetbrains.kotlinx.kover")
    }

    private fun applySpotless(target: Project) {
        val ktlintVersion = target.irisCatalog.version("ktlint")
        target.pluginManager.apply("com.diffplug.spotless")
        target.extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                // Composables are named like types (Compose convention); every other function stays camelCase.
                ktlint(ktlintVersion).editorConfigOverride(
                    mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable"),
                )
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint(ktlintVersion)
            }
        }
    }

    private fun applyDetekt(target: Project) {
        target.pluginManager.apply("dev.detekt")
        val config =
            target.tasks.register<WriteResourceTask>("writeDetektConfig") {
                content.set(readResource("detekt.yml"))
                output.set(target.layout.buildDirectory.file("iris/detekt.yml"))
            }
        target.extensions.configure<DetektExtension> {
            // Scan every source set (common, platform and test) rather than only the JVM defaults.
            source.setFrom(target.layout.projectDirectory.dir("src"))
            buildUponDefaultConfig.set(true)
            this.config.setFrom(config.flatMap { it.output })
        }
        // Plain `detekt` covers every source set but has no type resolution, so rules that need it (such as
        // LongParameterList) are skipped there. The per-compilation tasks (detektMainJvm, detektTestJvm, ...) run them.
        // The per-source-set tasks repeat plain `detekt` and stay out of `check`.
        target.pluginManager.withPlugin("lifecycle-base") {
            target.tasks.named("check") {
                dependsOn(target.tasks.withType<Detekt>().matching { !it.name.endsWith("SourceSet") })
            }
        }
    }

    private fun applySizeLimits(target: Project) {
        val sizeLimits =
            target.tasks.register<VerifySizeLimitsTask>("verifySizeLimits") {
                group = "verification"
                description = "Checks Kotlin file lengths against docs/coding-standards.md section 2."
                sources.from(target.fileTree("src") { include("**/*.kt") })
                moduleDirectory.set(target.layout.projectDirectory)
                report.set(target.layout.buildDirectory.file("reports/size-limits/offenders.txt"))
            }
        target.pluginManager.withPlugin("lifecycle-base") {
            target.tasks.named("check") { dependsOn(sizeLimits) }
        }
    }

    private fun applyLicenseCheck(target: Project) {
        // Resolved once, when the task graph is built (and stored in the configuration cache).
        val facts = lazy { collectLicenseFacts(target) }
        val licenseCheck =
            target.tasks.register<LicenseCheckTask>("licenseCheck") {
                group = "verification"
                description = "Checks the licenses of shipped and test dependencies against the D-020 allow-list."
                componentScopes.set(target.provider { facts.value.scopes })
                componentLicenses.set(target.provider { facts.value.licenses })
                report.set(target.layout.buildDirectory.file("reports/licenses/dependencies.txt"))
            }
        target.pluginManager.withPlugin("lifecycle-base") {
            target.tasks.named("check") { dependsOn(licenseCheck) }
        }
    }

    private fun readResource(name: String): String =
        requireNotNull(javaClass.getResource(name)) { "Resource $name is missing from build-logic" }.readText()
}
