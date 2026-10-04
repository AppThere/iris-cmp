package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.DataInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CompilerOptionsConventionTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `compiler warnings fail the build`() {
        val project =
            FixtureProject.kmpLibrary(dir).withCommonSource(
                "Deprecated.kt",
                """
                @Deprecated("use answer")
                public fun oldAnswer(): Int = 41

                public fun answer(): Int = oldAnswer() + 1
                """.trimIndent(),
            )

        val result = project.runner(":${FixtureProject.MODULE}:compileKotlinJvm").buildAndFail()

        assertContains(result.output, "is deprecated")
    }

    @Test
    fun `jvm classes target Java 17 bytecode`() {
        val project = FixtureProject.kmpLibrary(dir).withCommonSource("Answer.kt", "public fun answer(): Int = 42\n")

        project.runner(":${FixtureProject.MODULE}:compileKotlinJvm").build()

        val classFile = project.moduleDir.resolve("build/classes/kotlin/jvm/main/AnswerKt.class")
        assertEquals(JAVA_17_CLASS_MAJOR_VERSION, classMajorVersion(classFile))
    }

    private fun classMajorVersion(file: File): Int =
        DataInputStream(file.inputStream()).use { input ->
            input.readInt() // magic
            input.readUnsignedShort() // minor
            input.readUnsignedShort()
        }

    private companion object {
        const val JAVA_17_CLASS_MAJOR_VERSION = 61
    }
}
