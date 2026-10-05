package org.appthere.iris.testing

/** A small array-backed image for tests of the golden harness itself. */
internal class ArrayImage(
    override val width: Int,
    override val height: Int,
    override val channels: Int,
    val values: FloatArray = FloatArray(width * height * channels),
) : GoldenImage {
    override fun sample(
        x: Int,
        y: Int,
        channel: Int,
    ): Float = values[(y * width + x) * channels + channel]

    fun with(
        x: Int,
        y: Int,
        channel: Int,
        value: Float,
    ): ArrayImage =
        ArrayImage(
            width,
            height,
            channels,
            values.copyOf().also {
                it[(y * width + x) * channels + channel] =
                    value
            },
        )
}

/** Keeps everything in memory and records what the harness wrote. */
internal class MemoryGoldenStore(
    private val goldens: Map<String, GoldenImage> = emptyMap(),
) : GoldenStore {
    val candidates = mutableMapOf<String, GoldenImage>()
    val failures = mutableMapOf<String, Triple<GoldenImage, GoldenImage, GoldenImage>>()

    override fun readGolden(name: String): GoldenImage? = goldens[name]

    override fun writeCandidate(
        name: String,
        image: GoldenImage,
    ) {
        candidates[name] = image
    }

    override fun writeFailure(
        name: String,
        actual: GoldenImage,
        expected: GoldenImage,
        diff: GoldenImage,
    ) {
        failures[name] = Triple(actual, expected, diff)
    }
}
