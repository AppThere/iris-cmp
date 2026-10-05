package org.appthere.iris.buildlogic

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Links every iOS binary for [version] (D-024). Kotlin/Native's own default is higher (15.0 in 2.4.20);
 * `-Xoverride-konan-properties=minVersion.ios=` is the override documented on kotlinlang.org (native-target-support).
 */
internal fun KotlinMultiplatformExtension.targetIosMinimum(version: String) {
    targets.withType(KotlinNativeTarget::class.java).configureEach {
        binaries.configureEach {
            freeCompilerArgs += "-Xoverride-konan-properties=minVersion.ios=$version"
        }
    }
}
