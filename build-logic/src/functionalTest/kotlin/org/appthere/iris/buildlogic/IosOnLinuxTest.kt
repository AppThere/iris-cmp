package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What a non-Mac host can build for iOS (phase-0 section 4, item 5). Kotlin/Native compiles Apple klibs,
 * with the iOS platform libraries, on any host without an opt-in. Linking and simulator tests need macOS.
 */
class IosOnLinuxTest {
    @TempDir
    lateinit var dir: File

    private val module = ":${FixtureProject.MODULE}"

    @Test
    fun `ios main and test sources compile to klibs, including Foundation and UIKit calls`() {
        val project = FixtureProject.kmpLibrary(dir).withIosSources(IOS_PLATFORM_CODE)
        val tasks = IOS_TARGETS.flatMap { listOf("$module:compileKotlin$it", "$module:compileTestKotlin$it") }

        val result = project.runner(*tasks.toTypedArray()).build()

        tasks.forEach { assertEquals(TaskOutcome.SUCCESS, result.task(it)?.outcome, it) }
        listOf("iosArm64", "iosSimulatorArm64").forEach {
            val klib = project.moduleDir.resolve("build/classes/kotlin/$it/main/klib/${FixtureProject.MODULE}")
            assertTrue(klib.isDirectory, "Missing klib for $it at $klib")
        }
    }

    @Test
    fun `a type error in ios-only code fails the build`() {
        val project = FixtureProject.kmpLibrary(dir).withSource("iosMain", "Bad.kt", "public fun bad(): Int = platform.Foundation.NSDate()\n")

        val result = project.runner("$module:compileKotlinIosArm64").buildAndFail()

        assertContains(result.output, "Bad.kt")
        assertContains(result.output, "Return type mismatch")
    }

    @Test
    fun `composables compile for ios`() {
        val project =
            FixtureProject.kmpCompose(dir).withCommonSource(
                "Hello.kt",
                "import androidx.compose.runtime.Composable\n\n@Composable\npublic fun Hello() {}\n",
            )
        val tasks = IOS_TARGETS.map { "$module:compileKotlin$it" }

        val result = project.runner(*tasks.toTypedArray()).build()

        tasks.forEach { assertEquals(TaskOutcome.SUCCESS, result.task(it)?.outcome, it) }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun `on linux ios linking and simulator tests are skipped, not run`() {
        val project = FixtureProject.kmpLibrary(dir).withIosSources(IOS_PLATFORM_CODE)
        val link = "$module:linkDebugTestIosSimulatorArm64"
        val test = "$module:iosSimulatorArm64Test"

        val result = project.runner(link, test).build()

        assertEquals(TaskOutcome.SKIPPED, result.task(link)?.outcome)
        assertEquals(TaskOutcome.SKIPPED, result.task(test)?.outcome)
        assertContains(result.output, "simulator tests require macOS")
    }

    private fun FixtureProject.withIosSources(iosCode: String): FixtureProject =
        withCommonSource("Answer.kt", "public fun answer(): Int = 42\n")
            .withSource("iosMain", "Platform.kt", iosCode)
            .withSource(
                "commonTest",
                "AnswerTest.kt",
                """
                import kotlin.test.Test
                import kotlin.test.assertEquals

                class AnswerTest {
                    @Test
                    fun answerIs42() {
                        assertEquals(42, answer())
                    }
                }
                """.trimIndent(),
            )

    private companion object {
        val IOS_TARGETS = listOf("IosArm64", "IosSimulatorArm64")

        val IOS_PLATFORM_CODE =
            """
            import platform.Foundation.NSDate
            import platform.Foundation.timeIntervalSince1970
            import platform.UIKit.UIDevice

            public fun now(): Double = NSDate().timeIntervalSince1970

            public fun deviceName(): String = UIDevice.currentDevice.name
            """.trimIndent()
    }
}
