package s5

/** EXR pixel types (OpenEXR file layout, PixelType). */
enum class PixelType(val code: Int, val bytes: Int) { UINT(0, 4), HALF(1, 2), FLOAT(2, 4) }

/** EXR compression codes used here. */
enum class Compression(val code: Int, val linesPerBlock: Int) { NONE(0, 1), ZIPS(2, 1), ZIP(3, 16) }

data class Channel(val name: String, val type: PixelType)

/**
 * An image as raw sample bits per channel (16-bit half or 32-bit float/uint bits), row-major.
 * Channels are kept sorted by name, as EXR stores them.
 */
class ExrImage(val width: Int, val height: Int, channels: List<Channel>) {
    val channels = channels.sortedBy { it.name }
    val data: List<IntArray> = this.channels.map { IntArray(width * height) }

    fun bits(channel: Int, x: Int, y: Int) = data[channel][y * width + x]
}

internal class LeWriter(capacity: Int = 1024) {
    var buf = ByteArray(capacity)
    var size = 0

    fun u8(v: Int) = apply { ensure(1); buf[size++] = v.toByte() }
    fun i32(v: Int) = apply { u8(v); u8(v ushr 8); u8(v ushr 16); u8(v ushr 24) }
    fun i64(v: Long) = apply { i32(v.toInt()); i32((v ushr 32).toInt()) }
    fun f32(v: Float) = i32(v.toRawBits())
    fun str(s: String) = apply { s.encodeToByteArray().forEach { u8(it.toInt()) }; u8(0) }
    fun bytes(b: ByteArray) = apply { ensure(b.size); b.copyInto(buf, size); size += b.size }
    fun toByteArray() = buf.copyOf(size)

    private fun ensure(n: Int) { if (size + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, size + n)) }
}

internal class LeReader(val buf: ByteArray, var pos: Int = 0) {
    fun u8() = buf[pos++].toInt() and 0xff
    fun i32() = u8() or (u8() shl 8) or (u8() shl 16) or (u8() shl 24)
    fun i64() = (i32().toLong() and 0xffffffffL) or (i32().toLong() shl 32)
    fun f32() = Float.fromBits(i32())
    fun str(): String { val start = pos; while (buf[pos] != 0.toByte()) pos++; return buf.decodeToString(start, pos++) }
}

/** EXR's ZIP pre-processing: split even/odd bytes into two halves, then byte deltas (+128). */
internal fun zipPredict(raw: ByteArray): ByteArray {
    val t = ByteArray(raw.size)
    val half = (raw.size + 1) / 2
    for (i in raw.indices) if (i % 2 == 0) t[i / 2] = raw[i] else t[half + i / 2] = raw[i]
    for (i in t.size - 1 downTo 1) t[i] = (t[i] - t[i - 1] + 128).toByte()
    return t
}

internal fun zipUnpredict(t: ByteArray): ByteArray {
    for (i in 1 until t.size) t[i] = (t[i - 1] + t[i] - 128).toByte()
    val out = ByteArray(t.size)
    val half = (t.size + 1) / 2
    for (i in out.indices) out[i] = if (i % 2 == 0) t[i / 2] else t[half + i / 2]
    return out
}
