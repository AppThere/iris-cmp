package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidAppConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `android app builds a compose apk with the iris application id and sdk levels`() {
        val project =
            FixtureProject
                .androidApp(dir)
                .withFile(
                    "src/main/AndroidManifest.xml",
                    """
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                        <application android:label="Fixture">
                            <activity android:name=".MainActivity" android:exported="true">
                                <intent-filter>
                                    <action android:name="android.intent.action.MAIN" />
                                    <category android:name="android.intent.category.LAUNCHER" />
                                </intent-filter>
                            </activity>
                        </application>
                    </manifest>
                    """,
                )
                .withSource(
                    "main",
                    "org/appthere/iris/fixture/Screen.kt",
                    """
                    package org.appthere.iris.fixture

                    import androidx.compose.runtime.Composable

                    @Composable
                    fun Screen() {}
                    """.trimIndent(),
                ).withSource(
                    "main",
                    "org/appthere/iris/fixture/MainActivity.kt",
                    """
                    package org.appthere.iris.fixture

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent { Screen() }
                        }
                    }
                    """.trimIndent(),
                )

        project.runner(":${FixtureProject.MODULE}:assembleDebug").build()

        val apk = project.moduleDir.resolve("build/outputs/apk/debug/${FixtureProject.MODULE}-debug.apk")
        val badging = aapt2Badging(apk)
        assertContains(badging, "package: name='org.appthere.iris'")
        assertContains(badging, "minSdkVersion:'28'")
        assertContains(badging, "targetSdkVersion:'36'")
        val screen = project.moduleDir.resolve("build").walk().first { it.name == "ScreenKt.class" }
        assertTrue(COMPOSER in screen.readBytes().decodeToString(), "$screen was not compose-compiled")
    }

    @Test
    fun `android app compiles against an sdk new enough for compose multiplatform`() {
        val project =
            FixtureProject
                .androidApp(dir)
                .withFile("src/main/AndroidManifest.xml", "<manifest />")
                .withBuildScript("dependencies { implementation(libs.compose.ui) }")
        val task = ":${FixtureProject.MODULE}:checkDebugAarMetadata"

        val result = project.runner(task).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
    }

    private fun aapt2Badging(apk: File): String {
        val sdk = File(requireNotNull(System.getenv("ANDROID_HOME")) { "ANDROID_HOME is not set" })
        val aapt2 =
            sdk.resolve("build-tools").listFiles().orEmpty().sortedDescending().map { it.resolve("aapt2") }.first { it.canExecute() }
        val process = ProcessBuilder(aapt2.path, "dump", "badging", apk.path).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0) { "aapt2 failed: $output" }
        return output
    }

    private companion object {
        const val COMPOSER = "Landroidx/compose/runtime/Composer;"
    }
}
