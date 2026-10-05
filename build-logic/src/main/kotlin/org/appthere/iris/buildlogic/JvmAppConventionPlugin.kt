package org.appthere.iris.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.configure

/** Convention for JVM-only tools such as `iris-cli`: a Kotlin/JVM application whose main class is `MainKt` in the module's package. */
class JvmAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyIrisKotlinJvm()
        target.pluginManager.apply("application")
        target.pluginManager.apply(QualityConventionPlugin::class.java)
        target.extensions.configure<JavaApplication> {
            mainClass.set(modulePackage(target.name) + ".MainKt")
        }
    }
}
