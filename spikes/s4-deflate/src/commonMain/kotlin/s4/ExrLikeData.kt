package s4

import kotlin.math.sin
import kotlin.random.Random

const val TILE = 256
const val CHANNELS = 4
const val TILE_BYTES = TILE * TILE * CHANNELS * 2 // half floats

/**
 * A deterministic EXR-like tile: four planar half-float channels of a smooth gradient plus noise (a painted
 * tile, not white noise), then EXR's ZIP predictor (bytes split into two halves, then deltas), which is what
 * EXR feeds to Deflate.
 */
fun exrLikeTile(index: Int): ByteArray {
    val random = Random(index)
    val raw = ByteArray(TILE_BYTES)
    var o = 0
    for (c in 0 until CHANNELS) {
        for (y in 0 until TILE) {
            for (x in 0 until TILE) {
                val v = 0.5f + 0.4f * sin((x + index * 7) * 0.02f + c) * sin((y + index * 3) * 0.015f) + random.nextFloat() * 0.01f
                val h = toHalf(if (c == 3) 1f else v)
                raw[o++] = (h and 0xff).toByte()
                raw[o++] = (h shr 8).toByte()
            }
        }
    }
    return exrZipPredictor(raw)
}

private fun exrZipPredictor(raw: ByteArray): ByteArray {
    val split = ByteArray(raw.size)
    val half = (raw.size + 1) / 2
    for (i in raw.indices) if (i % 2 == 0) split[i / 2] = raw[i] else split[half + i / 2] = raw[i]
    for (i in split.size - 1 downTo 1) split[i] = (split[i] - split[i - 1] + 128).toByte()
    return split
}

/** Float to IEEE half (round to nearest even is not needed for test data). */
private fun toHalf(f: Float): Int {
    val bits = f.toRawBits()
    val sign = (bits ushr 16) and 0x8000
    val exp = ((bits ushr 23) and 0xff) - 127 + 15
    val mant = (bits ushr 13) and 0x3ff
    return when {
        exp <= 0 -> sign
        exp >= 31 -> sign or 0x7c00
        else -> sign or (exp shl 10) or mant
    }
}
