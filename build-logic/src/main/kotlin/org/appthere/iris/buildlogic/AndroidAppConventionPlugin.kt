package org.appthere.iris.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Convention for the Android app shell: an AGP application with built-in Kotlin and Compose.
 * AGP 9 compiles Kotlin itself, so `org.jetbrains.kotlin.android` is not applied (AGP fails the build if it is).
 */
class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        target.pluginManager.apply(QualityConventionPlugin::class.java)
        val catalog = target.irisCatalog
        target.extensions.configure<ApplicationExtension> {
            namespace = modulePackage(target.name)
            compileSdk = catalog.intVersion("android-compileSdk")
            defaultConfig {
                applicationId = IRIS_PACKAGE_ROOT
                minSdk = catalog.intVersion("android-minSdk")
                targetSdk = catalog.intVersion("android-targetSdk")
            }
            compileOptions {
                sourceCompatibility = IRIS_JAVA_VERSION
                targetCompatibility = IRIS_JAVA_VERSION
            }
        }
        target.extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                allWarningsAsErrors.set(true)
                jvmTarget.set(IRIS_JVM_TARGET)
            }
        }
        target.dependencies {
            add("implementation", catalog.library("androidx-activity-compose"))
        }
    }
}
