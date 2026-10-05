package org.appthere.iris.testing

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileGoldenStoreTest {
    private val root: File = Files.createTempDirectory("golden-store").toFile()
    private val goldens = root.resolve("testdata/golden/iris-render")
    private val build = root.resolve("iris-render/build")
    private val store = FileGoldenStore(goldenDirectory = goldens, buildDirectory = build, codec = TextCodec)
    private val image = ArrayImage(1, 2, 1, floatArrayOf(0.25f, 0.75f))

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun goldensAreReadFromTheModuleGoldenDirectory() {
        goldens.resolve("blend").mkdirs()
        goldens.resolve("blend/multiply.txt").writeBytes(TextCodec.encode(image))

        val read = store.readGolden("blend/multiply")

        assertEquals(listOf(0.25f, 0.75f), listOf(read!!.sample(0, 0, 0), read.sample(0, 1, 0)))
        assertNull(store.readGolden("blend/screen"))
    }

    @Test
    fun candidatesGoWhereGoldenUpdateLooks() {
        store.writeCandidate("blend/multiply", image)

        assertTrue(build.resolve("golden-candidates/blend/multiply.txt").isFile)
    }

    @Test
    fun failuresKeepActualExpectedAndDiffTogether() {
        store.writeFailure("blend/multiply", image, image, image)

        val dir = build.resolve("golden-failures/blend/multiply")
        assertEquals(setOf("actual.txt", "expected.txt", "diff.txt"), dir.list()!!.toSet())
    }

    @Test
    fun namesCannotEscapeTheirDirectories() {
        listOf("../secret", "/abs", "a//b", "", "a\\b", "dot.ted").forEach { name ->
            assertFailsWith<IllegalArgumentException>(name) { store.writeCandidate(name, image) }
        }
    }

    /** A readable stand-in until iris-exr provides the EXR codec. */
    private object TextCodec : GoldenCodec {
        override val extension: String = "txt"

        override fun encode(image: GoldenImage): ByteArray {
            val values =
                (0 until image.height).flatMap { y ->
                    (0 until image.width).flatMap { x ->
                        (
                            0 until
                                image.channels
                        ).map { image.sample(x, y, it) }
                    }
                }
            return "${image.width} ${image.height} ${image.channels}\n${values.joinToString(" ")}".encodeToByteArray()
        }

        override fun decode(bytes: ByteArray): GoldenImage {
            val (header, body) = bytes.decodeToString().split("\n")
            val (w, h, c) = header.split(" ").map { it.toInt() }
            return ArrayImage(w, h, c, body.split(" ").map { it.toFloat() }.toFloatArray())
        }
    }
}
