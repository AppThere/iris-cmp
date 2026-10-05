package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

/** D-026: iOS 15 is the minimum, pinned explicitly (it is also the Kotlin/Native 2.4.20 default). Linking needs macOS. */
class IosMinimumVersionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `library test binaries target ios 15`() {
        val project = FixtureProject.kmpLibrary(dir).withBuildScript(PRINT_BINARIES)

        val result = project.runner(":${FixtureProject.MODULE}:printBinaries").build()

        assertContains(result.output, "BINARIES=iosArm64:debugTest:true,iosSimulatorArm64:debugTest:true")
    }

    @Test
    fun `app frameworks target ios 15`() {
        val project = FixtureProject.iosApp(dir).withBuildScript(PRINT_BINARIES)

        val result = project.runner(":${FixtureProject.MODULE}:printBinaries").build()

        assertContains(
            result.output,
            "BINARIES=iosArm64:debugFramework:true,iosArm64:debugTest:true,iosArm64:releaseFramework:true," +
                "iosSimulatorArm64:debugFramework:true,iosSimulatorArm64:debugTest:true,iosSimulatorArm64:releaseFramework:true",
        )
    }

    private companion object {
        val PRINT_BINARIES =
            """
            tasks.register("printBinaries") {
                val binaries = kotlin.targets
                    .withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
                    .flatMap { target ->
                        target.binaries.map {
                            target.name + ":" + it.name + ":" +
                                ("-Xoverride-konan-properties=minVersion.ios=15.0" in it.freeCompilerArgs)
                        }
                    }
                    .sorted()
                    .joinToString(",")
                doLast { println("BINARIES=" + binaries) }
            }
            """
    }
}
