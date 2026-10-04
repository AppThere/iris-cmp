package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Convention for Iris library modules: Kotlin Multiplatform with the targets from docs/architecture.md section 1. */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        target.extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
            jvm()
            iosArm64()
            iosSimulatorArm64()
        }
    }
}
