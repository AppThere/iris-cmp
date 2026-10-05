package org.appthere.iris.buildlogic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoldenFilesTest {
    @TempDir
    lateinit var root: File

    private val golden get() = root.resolve("testdata/golden")
    private val pending get() = root.resolve("testdata/golden-pending")

    @Test
    fun `review lists pending adds and changes against the goldens, in path order`() {
        write(golden, "iris-render/multiply.exr", "old")
        write(golden, "iris-render/screen.exr", "same")
        write(pending, "iris-render/multiply.exr", "new")
        write(pending, "iris-render/screen.exr", "same")
        write(pending, "iris-brush/round.exr", "first")

        val changes = reviewGoldens(golden, pending)

        assertEquals(
            listOf(
                GoldenChange(GoldenChangeKind.ADD, "iris-brush/round.exr"),
                GoldenChange(GoldenChangeKind.CHANGE, "iris-render/multiply.exr"),
                GoldenChange(GoldenChangeKind.SAME, "iris-render/screen.exr"),
            ),
            changes,
        )
    }

    @Test
    fun `review of an empty or missing pending area is empty`() {
        write(golden, "iris-render/multiply.exr", "old")

        assertEquals(emptyList(), reviewGoldens(golden, pending))
    }

    @Test
    fun `update copies matching candidates to the pending area and never touches the goldens`() {
        val render = root.resolve("iris-render/build/golden-candidates")
        write(render, "multiply.exr", "candidate")
        write(render, "screen.exr", "other")
        write(golden, "iris-render/multiply.exr", "old")

        val promoted = promoteGoldenCandidates(mapOf("iris-render" to render), pending, "multiply")

        assertEquals(listOf("iris-render/multiply.exr"), promoted)
        assertEquals("candidate", pending.resolve("iris-render/multiply.exr").readText())
        assertFalse(pending.resolve("iris-render/screen.exr").exists())
        assertEquals("old", golden.resolve("iris-render/multiply.exr").readText())
    }

    @Test
    fun `names match by name, by module and name, and with wildcards`() {
        assertTrue(matchesGoldenName("iris-render", "blend/multiply.exr", "blend/multiply"))
        assertTrue(matchesGoldenName("iris-render", "blend/multiply.exr", "iris-render/blend/multiply"))
        assertTrue(matchesGoldenName("iris-render", "blend/multiply.exr", "blend/*"))
        assertTrue(matchesGoldenName("iris-render", "blend/multiply.exr", "*"))
        assertFalse(matchesGoldenName("iris-render", "blend/multiply.exr", "iris-brush/blend/multiply"))
        assertFalse(matchesGoldenName("iris-render", "blend/multiply.exr", "multiply"))
        assertFalse(matchesGoldenName("iris-render", "blend/multiply-2.exr", "blend/multiply"))
    }

    private fun write(
        dir: File,
        path: String,
        content: String,
    ) {
        val file = dir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
