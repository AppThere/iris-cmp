package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SizeLimitsTest {
    @TempDir
    lateinit var module: File

    @Test
    fun `a source file may have 400 lines but not 401`() {
        write("src/commonMain/kotlin/AtLimit.kt", 400)
        write("src/jvmMain/kotlin/Over.kt", 401)

        assertEquals(listOf("src/jvmMain/kotlin/Over.kt has 401 lines (limit 400)"), findOversizedFiles(module))
    }

    @Test
    fun `a test file may have 600 lines but not 601`() {
        write("src/commonTest/kotlin/AtLimitTest.kt", 600)
        write("src/androidHostTest/kotlin/OverTest.kt", 601)
        write("src/test/kotlin/UnitOverTest.kt", 601)

        assertEquals(
            listOf(
                "src/androidHostTest/kotlin/OverTest.kt has 601 lines (limit 600)",
                "src/test/kotlin/UnitOverTest.kt has 601 lines (limit 600)",
            ),
            findOversizedFiles(module),
        )
    }

    @Test
    fun `testdata fixtures and generated code are exempt`() {
        write("src/commonTest/kotlin/testdata/HugeFixture.kt", 5000)
        write("src/commonMain/kotlin/generated/Tables.kt", 5000)

        assertEquals(emptyList(), findOversizedFiles(module))
    }

    @Test
    fun `every offender is reported, in path order`() {
        write("src/commonMain/kotlin/B.kt", 450)
        write("src/commonMain/kotlin/A.kt", 401)

        assertEquals(
            listOf("src/commonMain/kotlin/A.kt has 401 lines (limit 400)", "src/commonMain/kotlin/B.kt has 450 lines (limit 400)"),
            findOversizedFiles(module),
        )
    }

    private fun write(
        path: String,
        lines: Int,
    ) {
        val file = module.resolve(path)
        file.parentFile.mkdirs()
        file.writeText((1..lines).joinToString("\n", postfix = "\n") { "// line $it" })
    }
}
