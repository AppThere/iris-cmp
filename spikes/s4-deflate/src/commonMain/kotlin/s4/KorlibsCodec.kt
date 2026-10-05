package s4

import korlibs.io.compression.CompressionContext
import korlibs.io.compression.compress
import korlibs.io.compression.deflate.DeflatePortable
import korlibs.io.compression.uncompress

/** korlibs-compression's pure-Kotlin Deflate (DeflatePortable), forced even on the JVM. Raw deflate only. */
object KorlibsCodec : Codec {
    override val name = "korlibs (pure Kotlin)"
    private val method = DeflatePortable(15)

    override fun deflate(input: ByteArray, level: Int, raw: Boolean): ByteArray {
        require(raw) { "zlib framing not wired for korlibs in this spike" }
        return method.compress(input, CompressionContext(level), input.size / 2)
    }

    override fun inflate(input: ByteArray, size: Int, raw: Boolean): ByteArray {
        require(raw)
        return method.uncompress(input, size)
    }
}

/** Own pure-Kotlin compressor (MiniDeflate) paired with korlibs' pure-Kotlin inflater. */
object PureKotlinCodec : Codec {
    override val name = "pure Kotlin (own + korlibs)"

    override fun deflate(input: ByteArray, level: Int, raw: Boolean): ByteArray = miniDeflate(input, level)

    override fun inflate(input: ByteArray, size: Int, raw: Boolean): ByteArray = KorlibsCodec.inflate(input, size, raw)
}
