package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GoldenReviewTaskTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `goldenUpdate stages a candidate and goldenReview lists it as a change`() {
        fixture()
        write("testdata/golden/iris-render/multiply.exr", "old")
        write("iris-render/build/golden-candidates/multiply.exr", "new")

        runner("goldenUpdate", "-Pname=multiply").build()
        val review = runner("goldenReview").build()

        assertEquals("new", dir.resolve("testdata/golden-pending/iris-render/multiply.exr").readText())
        assertEquals("old", dir.resolve("testdata/golden/iris-render/multiply.exr").readText())
        assertContains(review.output, "change  iris-render/multiply.exr")
    }

    @Test
    fun `goldenReview lists adds and reports when nothing is pending`() {
        fixture()
        write("testdata/golden-pending/iris-render/screen.exr", "first")

        val withAdd = runner("goldenReview", "--configuration-cache").build()
        dir.resolve("testdata/golden-pending").deleteRecursively()
        val empty = runner("goldenReview", "--configuration-cache").build()

        assertContains(withAdd.output, "add     iris-render/screen.exr")
        assertContains(empty.output, "No pending golden changes.")
    }

    @Test
    fun `goldenUpdate without a name or without a match fails`() {
        fixture()
        write("iris-render/build/golden-candidates/multiply.exr", "new")

        val noName = runner("goldenUpdate").buildAndFail()
        val noMatch = runner("goldenUpdate", "-Pname=screen").buildAndFail()

        assertContains(noName.output, "goldenUpdate needs -Pname=<name>")
        assertContains(noMatch.output, "No golden candidates match 'screen'")
    }

    private fun fixture() {
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture\"\ninclude(\":iris-render\")\n")
        dir.resolve("build.gradle.kts").writeText("plugins { id(\"iris.golden\") }\n")
        dir.resolve("iris-render").mkdirs()
    }

    private fun write(
        path: String,
        content: String,
    ) {
        val file = dir.resolve(path)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withTestKitDir(File(requireNotNull(System.getProperty("iris.testKitDir"))))
            .withArguments(*args, "--stacktrace")
            .forwardOutput()
}
