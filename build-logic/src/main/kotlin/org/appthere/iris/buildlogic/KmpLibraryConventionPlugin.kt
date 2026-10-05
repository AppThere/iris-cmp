package org.appthere.iris.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Convention for Iris library modules: Kotlin Multiplatform with the targets from docs/architecture.md section 1. */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        target.pluginManager.apply("com.android.kotlin.multiplatform.library")
        target.pluginManager.apply(QualityConventionPlugin::class.java)
        val catalog = target.extensions.getByType<VersionCatalogsExtension>().named("libs")
        target.extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
            compilerOptions {
                allWarningsAsErrors.set(true)
            }
            jvm {
                compilerOptions { jvmTarget.set(IRIS_JVM_TARGET) }
            }
            iosArm64()
            iosSimulatorArm64()
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                namespace = modulePackage(target.name)
                compileSdk = catalog.intVersion("android-compileSdk")
                minSdk = catalog.intVersion("android-minSdk")
                compilerOptions { jvmTarget.set(IRIS_JVM_TARGET) }
                withHostTest {}
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    private fun VersionCatalog.intVersion(alias: String): Int =
        findVersion(alias)
            .orElseThrow { IllegalStateException("Version '$alias' is missing from libs.versions.toml") }
            .requiredVersion
            .toInt()
}
