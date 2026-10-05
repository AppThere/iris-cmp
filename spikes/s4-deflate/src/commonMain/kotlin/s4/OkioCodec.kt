package s4

import okio.Buffer
import okio.Deflater
import okio.DeflaterSink
import okio.Inflater
import okio.InflaterSource
import okio.buffer
import okio.use

/** Okio: java.util.zip on the JVM (type aliases), the system zlib through platform.zlib on Kotlin/Native. */
object OkioCodec : Codec {
    override val name = "okio (platform zlib)"

    override fun deflate(input: ByteArray, level: Int, raw: Boolean): ByteArray {
        val out = Buffer()
        DeflaterSink(out, Deflater(level, raw)).use { sink ->
            val source = Buffer().write(input)
            sink.write(source, source.size)
        }
        return out.readByteArray()
    }

    override fun inflate(input: ByteArray, size: Int, raw: Boolean): ByteArray =
        InflaterSource(Buffer().write(input), Inflater(raw)).buffer().use { it.readByteArray(size.toLong()) }
}
