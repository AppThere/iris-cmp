package org.appthere.iris.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

// Bytecode level for every JVM and Android target; the build itself may run on a newer JDK.
internal val IRIS_JVM_TARGET = JvmTarget.JVM_17
internal val IRIS_JAVA_VERSION = JavaVersion.VERSION_17

/** Package root of every Iris module, and the Android application id. A placeholder until P-001 is settled. */
internal const val IRIS_PACKAGE_ROOT = "org.appthere.iris"

/** `iris-platform-input` becomes `org.appthere.iris.platform.input`; `app-desktop` becomes `org.appthere.iris.app.desktop`. */
internal fun modulePackage(moduleName: String): String =
    "$IRIS_PACKAGE_ROOT." + moduleName.removePrefix("iris-").replace('-', '.')

/** Applies Kotlin/JVM with Iris bytecode level, warnings as errors and kotlin-test, for Kotlin and Java sources alike. */
internal fun Project.applyIrisKotlinJvm() {
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            allWarningsAsErrors.set(true)
            jvmTarget.set(IRIS_JVM_TARGET)
        }
    }
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = IRIS_JAVA_VERSION
        targetCompatibility = IRIS_JAVA_VERSION
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(IRIS_JAVA_VERSION.majorVersion.toInt())
    }
    // kotlin-test on JUnit 5; the Kotlin plugin picks the matching kotlin-test-junit5 variant.
    dependencies.add("testImplementation", dependencies.kotlin("test"))
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
