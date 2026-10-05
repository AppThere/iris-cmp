package org.appthere.iris.testing

/**
 * Read access to rendered output for golden comparison (docs/testing-tdd.md section 4).
 * Adapters wrap tile buffers without copying their pixels.
 */
public interface GoldenImage {
    public val width: Int
    public val height: Int
    public val channels: Int

    public fun sample(
        x: Int,
        y: Int,
        channel: Int,
    ): Float
}
