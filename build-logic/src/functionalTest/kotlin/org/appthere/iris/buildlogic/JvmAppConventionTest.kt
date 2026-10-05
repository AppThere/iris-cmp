package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class JvmAppConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `jvm app runs MainKt from the module package, built as java 17 bytecode`() {
        val project =
            FixtureProject.jvmApp(dir).withSource(
                "main",
                "org/appthere/iris/fixture/Main.kt",
                """
                package org.appthere.iris.fixture

                fun main() {
                    println("HELLO=" + Runtime.version().feature())
                }
                """.trimIndent(),
            )

        val result = project.runner(":${FixtureProject.MODULE}:run").build()

        assertContains(result.output, "HELLO=")
        val mainKt = project.moduleDir.resolve("build/classes/kotlin/main/org/appthere/iris/fixture/MainKt.class")
        val bytes = mainKt.readBytes()
        val major = (bytes[6].toInt() and 0xff shl 8) or (bytes[7].toInt() and 0xff)
        assertEquals(JAVA_17_CLASS_FILE, major)
    }

    @Test
    fun `jvm app treats warnings as errors`() {
        val project =
            FixtureProject.jvmApp(dir).withSource(
                "main",
                "org/appthere/iris/fixture/Main.kt",
                """
                package org.appthere.iris.fixture

                @Deprecated("use answer")
                fun oldAnswer(): Int = 41

                fun main() {
                    println(oldAnswer())
                }
                """.trimIndent(),
            )

        val result = project.runner(":${FixtureProject.MODULE}:compileKotlin").buildAndFail()

        assertContains(result.output, "is deprecated")
    }

    @Test
    fun `jvm app tests use kotlin test on the JUnit platform`() {
        val project =
            FixtureProject
                .jvmApp(dir)
                .withSource("main", "org/appthere/iris/fixture/Main.kt", "package org.appthere.iris.fixture\n\nfun answer() = 42\n")
                .withSource(
                    "test",
                    "org/appthere/iris/fixture/AnswerTest.kt",
                    """
                    package org.appthere.iris.fixture

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

        project.runner(":${FixtureProject.MODULE}:test").build()

        val report = project.moduleDir.resolve("build/test-results/test/TEST-org.appthere.iris.fixture.AnswerTest.xml")
        assertContains(report.readText(), "tests=\"1\"")
    }

    private companion object {
        const val JAVA_17_CLASS_FILE = 61
    }
}
