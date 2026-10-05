package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** Linking the framework and running the app need macOS; these tests cover what any host can check. */
class IosAppConventionTest {
    @TempDir
    lateinit var dir: File

    private val module = ":${FixtureProject.MODULE}"

    @Test
    fun `ios app declares only the ios targets, each with a static IrisApp framework for xcode`() {
        val project =
            FixtureProject.iosApp(dir).withBuildScript(
                """
                tasks.register("printIos") {
                    val targets = kotlin.targets.names.sorted().joinToString(",")
                    val frameworks = kotlin.targets
                        .withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
                        .flatMap { target ->
                            target.binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>()
                                .map { target.name + ":" + it.baseName + ":static=" + it.isStatic + ":" + it.buildType }
                        }
                        .sorted()
                        .joinToString(",")
                    val embed = "embedAndSignAppleFrameworkForXcode" in tasks.names
                    doLast {
                        println("TARGETS=" + targets)
                        println("FRAMEWORKS=" + frameworks)
                        println("EMBED=" + embed)
                    }
                }
                """,
            )

        val result = project.runner("$module:printIos").build()

        assertContains(result.output, "TARGETS=iosArm64,iosSimulatorArm64,metadata")
        assertContains(
            result.output,
            "FRAMEWORKS=" +
                "iosArm64:IrisApp:static=true:DEBUG,iosArm64:IrisApp:static=true:RELEASE," +
                "iosSimulatorArm64:IrisApp:static=true:DEBUG,iosSimulatorArm64:IrisApp:static=true:RELEASE",
        )
        assertContains(result.output, "EMBED=true")
    }

    @Test
    fun `a compose ui view controller compiles for both ios targets`() {
        val project =
            FixtureProject.iosApp(dir).withSource(
                "iosMain",
                "org/appthere/iris/fixture/MainViewController.kt",
                """
                package org.appthere.iris.fixture

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.window.ComposeUIViewController
                import platform.UIKit.UIViewController

                @Composable
                fun Screen() {}

                fun mainViewController(): UIViewController = ComposeUIViewController { Screen() }
                """.trimIndent(),
            )
        val tasks = listOf("$module:compileKotlinIosArm64", "$module:compileKotlinIosSimulatorArm64")

        val result = project.runner(*tasks.toTypedArray()).build()

        tasks.forEach { assertEquals(TaskOutcome.SUCCESS, result.task(it)?.outcome, it) }
    }
}
