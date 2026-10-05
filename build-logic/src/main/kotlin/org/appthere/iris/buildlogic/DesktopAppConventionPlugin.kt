package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Convention for the desktop app shell: a Kotlin/JVM Compose desktop application for the build host.
 * The main class is `MainKt` in the module's package (`app-desktop` uses `org.appthere.iris.app.desktop`).
 */
class DesktopAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.jvm")
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        target.pluginManager.apply("org.jetbrains.compose")
        target.pluginManager.apply(QualityConventionPlugin::class.java)
        target.extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                allWarningsAsErrors.set(true)
                jvmTarget.set(IRIS_JVM_TARGET)
            }
        }
        target.extensions.configure<JavaPluginExtension> {
            sourceCompatibility = IRIS_JAVA_VERSION
            targetCompatibility = IRIS_JAVA_VERSION
        }
        target.tasks.withType<JavaCompile>().configureEach {
            options.release.set(IRIS_JAVA_VERSION.majorVersion.toInt())
        }
        val composeVersion =
            target.extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")
                .findVersion("compose-multiplatform")
                .orElseThrow { IllegalStateException("Version 'compose-multiplatform' is missing from libs.versions.toml") }
                .requiredVersion
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
