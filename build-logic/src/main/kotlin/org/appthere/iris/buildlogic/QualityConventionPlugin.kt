package org.appthere.iris.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Formatting (Spotless with ktlint), static analysis (detekt) and coverage (Kover), all wired into `check`. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val ktlintVersion = target.irisCatalog.version("ktlint")

        target.pluginManager.apply("com.diffplug.spotless")
        target.extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                ktlint(ktlintVersion)
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint(ktlintVersion)
            }
        }

        target.pluginManager.apply("dev.detekt")
        target.extensions.configure<DetektExtension> {
            // Scan every source set (common, platform and test) rather than only the JVM defaults.
            source.setFrom(target.layout.projectDirectory.dir("src"))
            buildUponDefaultConfig.set(true)
        }

        target.pluginManager.apply("org.jetbrains.kotlinx.kover")
    }
}
