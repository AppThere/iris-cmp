package s4

import kotlin.time.Duration
import kotlin.time.TimeSource

/** Round-trips [totalBytes] of EXR-like tiles through [codec]; checks every tile byte for byte. */
fun benchDeflate(codec: Codec, totalBytes: Long, level: Int): String {
    val tiles = (totalBytes / TILE_BYTES).toInt()
    val samples = List(16) { exrLikeTile(it) } // generation is not what we measure
    var compressedTotal = 0L
    var deflateTime = Duration.ZERO
    var inflateTime = Duration.ZERO
    for (i in 0 until tiles) {
        val tile = samples[i % samples.size]
        val t0 = TimeSource.Monotonic.markNow()
        val packed = codec.deflate(tile, level, raw = true)
        val t1 = TimeSource.Monotonic.markNow()
        val unpacked = codec.inflate(packed, tile.size, raw = true)
        inflateTime += t1.elapsedNow()
        deflateTime += t1 - t0
        check(unpacked.contentEquals(tile)) { "${codec.name}: round trip differs at tile $i" }
        compressedTotal += packed.size
    }
    val mib = tiles.toLong() * TILE_BYTES / (1024.0 * 1024.0)
    val ratio = tiles.toLong() * TILE_BYTES.toDouble() / compressedTotal
    return "${codec.name.padEnd(24)} level $level: ${tiles} tiles (${mib.toInt()} MiB) " +
        "deflate ${rate(mib, deflateTime)} MiB/s, inflate ${rate(mib, inflateTime)} MiB/s, ratio ${round2(ratio)}"
}

private fun rate(mib: Double, d: Duration): String = round2(mib / (d.inWholeMicroseconds / 1e6)).toString()

private fun round2(x: Double): Double = kotlin.math.round(x * 100) / 100
