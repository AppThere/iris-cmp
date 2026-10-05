package s5

import okio.Buffer
import okio.Deflater
import okio.DeflaterSink
import okio.Inflater
import okio.InflaterSource
import okio.buffer
import okio.use

/** zlib-format Deflate (EXR ZIP uses zlib's compress()), through Okio as decided in D-028. */
object Zlib {
    fun compress(input: ByteArray, level: Int): ByteArray {
        val out = Buffer()
        DeflaterSink(out, Deflater(level, false)).use { sink -> sink.write(Buffer().write(input), input.size.toLong()) }
        return out.readByteArray()
    }

    fun uncompress(input: ByteArray, size: Int): ByteArray =
        InflaterSource(Buffer().write(input), Inflater(false)).buffer().use { it.readByteArray(size.toLong()) }
}
