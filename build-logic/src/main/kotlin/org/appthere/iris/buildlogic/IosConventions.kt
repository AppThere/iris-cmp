package org.appthere.iris.buildlogic

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Links every iOS binary for [version] (D-026), pinned explicitly rather than inherited from Kotlin/Native's default,
 * so a changed default shows up in CI's minos check. Uses the override documented on kotlinlang.org (native-target-support).
 * Debug builds also link Kotlin/Native's prebuilt platform caches, which are built for its default; keep [version] equal to it.
 */
internal fun KotlinMultiplatformExtension.targetIosMinimum(version: String) {
    targets.withType(KotlinNativeTarget::class.java).configureEach {
        binaries.configureEach {
            freeCompilerArgs += "-Xoverride-konan-properties=minVersion.ios=$version"
        }
    }
}
