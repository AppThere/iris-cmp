package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension

/**
 * Convention for the desktop app shell: a Kotlin/JVM Compose desktop application for the build host.
 * The main class is `MainKt` in the module's package (`app-desktop` uses `org.appthere.iris.app.desktop`).
 */
class DesktopAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyIrisKotlinJvm()
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        target.pluginManager.apply("org.jetbrains.compose")
        target.pluginManager.apply(QualityConventionPlugin::class.java)
        val composeVersion = target.irisCatalog.version("compose-multiplatform")
        val artifact = composeDesktopArtifact(System.getProperty("os.name"), System.getProperty("os.arch"))
        target.dependencies {
            add("implementation", "org.jetbrains.compose.desktop:$artifact:$composeVersion")
        }
        target.extensions.getByType<ComposeExtension>().extensions.configure<DesktopExtension> {
            application {
                mainClass = modulePackage(target.name) + ".MainKt"
            }
        }
    }
}
