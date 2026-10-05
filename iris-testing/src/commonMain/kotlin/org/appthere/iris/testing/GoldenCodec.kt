package org.appthere.iris.testing

/** Turns golden images into file bytes and back. The EXR codec arrives with `iris-exr`. */
public interface GoldenCodec {
    /** File extension without the dot, such as `exr`. */
    public val extension: String

    public fun encode(image: GoldenImage): ByteArray

    public fun decode(bytes: ByteArray): GoldenImage
}
