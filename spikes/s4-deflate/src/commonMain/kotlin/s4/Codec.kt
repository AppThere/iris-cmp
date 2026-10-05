package s4

/** A Deflate implementation under test. [raw] means no zlib header (ZIP entries); otherwise zlib format (EXR ZIP). */
interface Codec {
    val name: String

    fun deflate(input: ByteArray, level: Int, raw: Boolean): ByteArray

    fun inflate(input: ByteArray, size: Int, raw: Boolean): ByteArray
}
