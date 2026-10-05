package s8

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun rssMib(): Long = File("/proc/self/status").readLines().first { it.startsWith("VmRSS:") }.split(Regex("\\s+"))[1].toLong() / 1024

fun benchHeap(iterations: Int): String {
    val src = ShortArray(SAMPLES)
    val dst = ShortArray(SAMPLES)
    fillTiles({ i, v -> src[i] = v.toShort() }, { i, v -> dst[i] = v.toShort() })
    return benchBlend("JVM heap ShortArray (baseline)", iterations, { dst.sumOf { it.toLong() and 0xffff } }) {
        blendTile({ src[it].toInt() and 0xffff }, { dst[it].toInt() and 0xffff }, { i, v -> dst[i] = v.toShort() })
    }
}

fun benchDirect(iterations: Int): String {
    val src = ByteBuffer.allocateDirect(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN)
    val dst = ByteBuffer.allocateDirect(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN)
    fillTiles({ i, v -> src.putShort(i * 2, v.toShort()) }, { i, v -> dst.putShort(i * 2, v.toShort()) })
    return benchBlend("direct ByteBuffer", iterations, { (0 until SAMPLES).sumOf { dst.getShort(it * 2).toLong() and 0xffff } }) {
        blendTile({ src.getShort(it * 2).toInt() and 0xffff }, { dst.getShort(it * 2).toInt() and 0xffff }, { i, v -> dst.putShort(i * 2, v.toShort()) })
    }
}

fun benchDirectShortView(iterations: Int): String {
    val src = ByteBuffer.allocateDirect(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    val dst = ByteBuffer.allocateDirect(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    fillTiles({ i, v -> src.put(i, v.toShort()) }, { i, v -> dst.put(i, v.toShort()) })
    return benchBlend("direct ByteBuffer, ShortBuffer view", iterations, { (0 until SAMPLES).sumOf { dst.get(it).toLong() and 0xffff } }) {
        blendTile({ src.get(it).toInt() and 0xffff }, { dst.get(it).toInt() and 0xffff }, { i, v -> dst.put(i, v.toShort()) })
    }
}

/** Allocates and drops [count] 512 KiB direct buffers; only the GC frees them. */
fun leakDirectDropped(count: Int): String {
    val start = rssMib()
    var peak = start
    val t = kotlin.time.TimeSource.Monotonic.markNow()
    repeat(count) {
        ByteBuffer.allocateDirect(SAMPLES * 2).touchEveryPage()
        if (it % 1000 == 0) peak = maxOf(peak, rssMib())
    }
    return "direct ByteBuffer, dropped for the GC: $count x 512 KiB in ${t.elapsedNow().inWholeMilliseconds} ms; RSS $start -> peak $peak MiB -> end ${rssMib()} MiB"
}

/** Allocates [count] tiles through a small reuse pool, as TileBuffer pooling would. */
fun leakDirectPooled(count: Int): String {
    val pool = ArrayDeque<ByteBuffer>()
    val start = rssMib()
    var peak = start
    val t = kotlin.time.TimeSource.Monotonic.markNow()
    repeat(count) {
        val b = pool.removeFirstOrNull() ?: ByteBuffer.allocateDirect(SAMPLES * 2)
        b.touchEveryPage()
        if (pool.size < 64) pool.addLast(b)
        if (it % 1000 == 0) peak = maxOf(peak, rssMib())
    }
    return "direct ByteBuffer, pooled: $count x 512 KiB in ${t.elapsedNow().inWholeMilliseconds} ms; RSS $start -> peak $peak MiB -> end ${rssMib()} MiB"
}

fun benchDirectPacked(iterations: Int): String {
    val src = ByteBuffer.allocateDirect(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN)
    val dst = ByteBuffer.allocateDirect(SAMPLES * 2).order(ByteOrder.LITTLE_ENDIAN)
    fillTiles({ i, v -> src.putShort(i * 2, v.toShort()) }, { i, v -> dst.putShort(i * 2, v.toShort()) })
    return benchBlend("direct ByteBuffer, packed pixel", iterations, { (0 until SAMPLES).sumOf { dst.getShort(it * 2).toLong() and 0xffff } }) {
        blendTilePacked({ src.getLong(it * 8) }, { dst.getLong(it * 8) }, { p, v -> dst.putLong(p * 8, v) })
    }
}

/** Writes one byte per 4 KiB page, as real tile data would, so lazily mapped pages become resident. */
fun ByteBuffer.touchEveryPage(): ByteBuffer {
    var i = 0
    while (i < capacity()) { put(i, 1); i += 4096 }
    return this
}
