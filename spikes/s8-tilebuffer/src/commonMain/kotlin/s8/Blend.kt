package s8

const val TILE = 256
const val SAMPLES = TILE * TILE * 4

/**
 * Premultiplied src-over of one Rgba16F tile onto another: dst = src + dst * (1 - srcA), per channel in float.
 * Inline with inline accessors, so each memory backend gets its own loop without virtual calls or allocation.
 */
inline fun blendTile(src: (Int) -> Int, dst: (Int) -> Int, store: (Int, Int) -> Unit) {
    val table = Half.toFloat
    var i = 0
    while (i < SAMPLES) {
        val inv = 1f - table[src(i + 3)]
        store(i, Half.fromFloat(table[src(i)] + table[dst(i)] * inv))
        store(i + 1, Half.fromFloat(table[src(i + 1)] + table[dst(i + 1)] * inv))
        store(i + 2, Half.fromFloat(table[src(i + 2)] + table[dst(i + 2)] * inv))
        store(i + 3, Half.fromFloat(table[src(i + 3)] + table[dst(i + 3)] * inv))
        i += 4
    }
}

/** A semi-transparent premultiplied source pattern and an opaque destination, as half bits. */
inline fun fillTiles(setSrc: (Int, Int) -> Unit, setDst: (Int, Int) -> Unit) {
    for (p in 0 until TILE * TILE) {
        val a = 0.25f + (p % 97) / 194f
        for (c in 0 until 3) setSrc(p * 4 + c, Half.fromFloat(a * ((p + c * 31) % 255) / 255f))
        setSrc(p * 4 + 3, Half.fromFloat(a))
        for (c in 0 until 3) setDst(p * 4 + c, Half.fromFloat(((p * 7 + c) % 255) / 255f))
        setDst(p * 4 + 3, Half.fromFloat(1f))
    }
}

/** Runs [blendOnce] [iterations] times after a warm-up and reports throughput. */
inline fun benchBlend(name: String, iterations: Int, checksum: () -> Long, blendOnce: () -> Unit): String {
    repeat(iterations / 4) { blendOnce() }
    val t = kotlin.time.TimeSource.Monotonic.markNow()
    repeat(iterations) { blendOnce() }
    val ns = t.elapsedNow().inWholeNanoseconds.toDouble()
    val perTileUs = ns / iterations / 1000
    val mpix = TILE * TILE * iterations / (ns / 1e9) / 1e6
    return "${name.padEnd(28)} ${round1(perTileUs)} us per 256x256 tile, ${round1(mpix)} Mpixel/s (checksum ${checksum()})"
}

fun round1(x: Double) = kotlin.math.round(x * 10) / 10

/** The same blend, reading and writing one RGBA16F pixel as a 64-bit word (four halves) per access. */
inline fun blendTilePacked(srcPixel: (Int) -> Long, dstPixel: (Int) -> Long, storePixel: (Int, Long) -> Unit) {
    val table = Half.toFloat
    for (p in 0 until TILE * TILE) {
        val s = srcPixel(p)
        val d = dstPixel(p)
        val inv = 1f - table[((s ushr 48) and 0xffff).toInt()]
        var out = 0L
        for (c in 0 until 4) {
            val shift = c * 16
            val v = table[((s ushr shift) and 0xffff).toInt()] + table[((d ushr shift) and 0xffff).toInt()] * inv
            out = out or (Half.fromFloat(v).toLong() shl shift)
        }
        storePixel(p, out)
    }
}
