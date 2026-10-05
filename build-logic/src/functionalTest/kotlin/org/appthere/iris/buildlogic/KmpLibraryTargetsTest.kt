package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class KmpLibraryTargetsTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `library declares the jvm, android and ios targets`() {
        val project =
            FixtureProject.kmpLibrary(dir).withBuildScript(
                """
                tasks.register("printTargets") {
                    val names = kotlin.targets.names.sorted().joinToString(",")
                    doLast { println("TARGETS=" + names) }
                }
                """,
            )

        val result = project.runner(":${FixtureProject.MODULE}:printTargets").build()

        assertContains(result.output, "TARGETS=android,iosArm64,iosSimulatorArm64,jvm,metadata")
    }

    @Test
    fun `android target uses min SDK 28 and a namespace derived from the module name`() {
        val project =
            FixtureProject.kmpLibrary(dir).withBuildScript(
                """
                tasks.register("printAndroid") {
                    val android = kotlin.targets
                        .withType<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>()
                        .single()
                    val line = "ANDROID=" + android.namespace + "," + android.minSdk + "," + android.compileSdk
                    doLast { println(line) }
                }
                """,
            )

        val result = project.runner(":${FixtureProject.MODULE}:printAndroid").build()

        assertContains(result.output, "ANDROID=org.appthere.iris.fixture,28,37")
    }
}
