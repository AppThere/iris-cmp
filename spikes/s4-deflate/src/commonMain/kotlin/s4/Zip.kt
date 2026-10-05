package s4

/** Where the writer puts bytes; the spike's JVM adapter wraps a buffered file stream. */
interface ZipSink {
    val position: Long
    fun write(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size)
}

/** Random-access reads for the reader; the JVM adapter wraps a RandomAccessFile. */
interface ZipSource {
    val size: Long
    fun read(position: Long, into: ByteArray, offset: Int = 0, length: Int = into.size)
}

enum class Method(val code: Int) { STORED(0), DEFLATED(8) }

class ZipEntry(
    val name: String,
    val method: Method,
    val crc32: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long,
)

internal const val MAX16 = 0xFFFF
internal const val MAX32 = 0xFFFFFFFFL
internal const val LOCAL_SIG = 0x04034b50
internal const val CENTRAL_SIG = 0x02014b50
internal const val EOCD_SIG = 0x06054b50
internal const val ZIP64_EOCD_SIG = 0x06064b50
internal const val ZIP64_LOCATOR_SIG = 0x07064b50
internal const val ZIP64_EXTRA = 0x0001
internal const val DOS_TIME = 0 // 00:00:00, fixed for deterministic output
internal const val DOS_DATE = (46 shl 9) or (1 shl 5) or 1 // 2026-01-01

/** Little-endian byte building. */
internal class Bytes {
    private var buf = ByteArray(64)
    var size = 0
        private set

    fun u16(v: Int) = apply { put(v); put(v ushr 8) }
    fun u32(v: Int) = apply { u16(v and 0xffff); u16(v ushr 16) }
    fun u32(v: Long) = u32(v.toInt())
    fun u64(v: Long) = apply { u32(v.toInt()); u32((v ushr 32).toInt()) }
    fun bytes(b: ByteArray) = apply { b.forEach { put(it.toInt()) } }
    fun toByteArray() = buf.copyOf(size)

    private fun put(v: Int) {
        if (size == buf.size) buf = buf.copyOf(buf.size * 2)
        buf[size++] = v.toByte()
    }
}

internal fun ByteArray.u16(at: Int) = (this[at].toInt() and 0xff) or ((this[at + 1].toInt() and 0xff) shl 8)
internal fun ByteArray.u32(at: Int) = u16(at) or (u16(at + 2) shl 16)
internal fun ByteArray.u32L(at: Int) = u32(at).toLong() and MAX32
internal fun ByteArray.u64(at: Int) = u32L(at) or (u32L(at + 4) shl 32)
