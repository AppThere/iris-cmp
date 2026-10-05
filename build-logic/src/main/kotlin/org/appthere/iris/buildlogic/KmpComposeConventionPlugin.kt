package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Convention for Iris library modules with Compose UI: `iris.kmp.library` plus the Compose compiler and runtime. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(KmpLibraryConventionPlugin::class.java)
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val runtime =
            target.extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")
                .findLibrary("compose-runtime")
                .orElseThrow { IllegalStateException("Library 'compose-runtime' is missing from libs.versions.toml") }
        target.extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(runtime.get())
            }
        }
    }
}
