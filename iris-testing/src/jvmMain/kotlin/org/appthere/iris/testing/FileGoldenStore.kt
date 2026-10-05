package org.appthere.iris.testing

import java.io.File

/**
 * Goldens on disk (docs/testing-tdd.md section 4): read from [goldenDirectory] (`testdata/golden/<module>`),
 * candidates to `<buildDirectory>/golden-candidates/`, failures to `<buildDirectory>/golden-failures/<name>/`.
 */
public class FileGoldenStore(
    private val goldenDirectory: File,
    private val buildDirectory: File,
    private val codec: GoldenCodec,
) : GoldenStore {
    override fun readGolden(name: String): GoldenImage? {
        requireValidGoldenName(name)
        val file = goldenDirectory.resolve("$name.${codec.extension}")
        return if (file.isFile) codec.decode(file.readBytes()) else null
    }

    override fun writeCandidate(
        name: String,
        image: GoldenImage,
    ) {
        requireValidGoldenName(name)
        write(buildDirectory.resolve("golden-candidates/$name.${codec.extension}"), image)
    }

    override fun writeFailure(
        name: String,
        actual: GoldenImage,
        expected: GoldenImage,
        diff: GoldenImage,
    ) {
        requireValidGoldenName(name)
        val dir = buildDirectory.resolve("golden-failures/$name")
        write(dir.resolve("actual.${codec.extension}"), actual)
        write(dir.resolve("expected.${codec.extension}"), expected)
        write(dir.resolve("diff.${codec.extension}"), diff)
    }

    private fun write(
        file: File,
        image: GoldenImage,
    ) {
        file.parentFile.mkdirs()
        file.writeBytes(codec.encode(image))
    }
}
