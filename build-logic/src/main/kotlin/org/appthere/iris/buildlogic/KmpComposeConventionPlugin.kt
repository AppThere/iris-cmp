package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention for Iris library modules with Compose UI: `iris.kmp.library` plus the Compose compiler and runtime.
 * `jvmTest` gets Compose `ui-test` and the host's desktop runtime, so UI tests run on the desktop JVM.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(KmpLibraryConventionPlugin::class.java)
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val runtime = target.irisCatalog.library("compose-runtime")
        val catalog = target.irisCatalog
        // Compose UI tests run on the desktop JVM, which needs the host's Skia runtime.
        val desktopRuntime =
            "org.jetbrains.compose.desktop:" +
                composeDesktopArtifact(System.getProperty("os.name"), System.getProperty("os.arch")) + ":" +
                catalog.version("compose-multiplatform")
        target.extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(runtime.get())
            }
            sourceSets.getByName("jvmTest").dependencies {
                implementation(catalog.library("compose-ui-test").get())
                implementation(desktopRuntime)
            }
        }
    }
}
