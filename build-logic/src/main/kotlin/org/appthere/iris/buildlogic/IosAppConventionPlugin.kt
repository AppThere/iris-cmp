package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention for the iOS app shell: a static Compose framework named [FRAMEWORK_NAME] for each iOS target.
 * The Xcode project embeds it through `embedAndSignAppleFrameworkForXcode`; linking needs macOS.
 * The minimum iOS version comes from the catalog (D-024).
 */
class IosAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        target.pluginManager.apply(QualityConventionPlugin::class.java)
        val catalog = target.irisCatalog
        target.extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
                allWarningsAsErrors.set(true)
            }
            listOf(iosArm64(), iosSimulatorArm64()).forEach { ios ->
                ios.binaries.framework {
                    baseName = FRAMEWORK_NAME
                    isStatic = true
                }
            }
            targetIosMinimum(catalog.version("ios-minVersion"))
            // Only iOS targets exist here, so commonMain is iOS-only; iosMain is created later, by the default hierarchy.
            sourceSets.getByName("commonMain").dependencies {
                implementation(catalog.library("compose-runtime").get())
                implementation(catalog.library("compose-ui").get())
            }
        }
    }

    private companion object {
        const val FRAMEWORK_NAME = "IrisApp"
    }
}
