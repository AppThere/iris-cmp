package s7

import java.io.File
import kotlin.math.pow

/** The parts of an ICC profile the prototype reads (ICC.1:2010 / ICC.1:2001 v2). */
class Profile(val bytes: ByteArray) {
    val colorSpace = sig(16)
    val pcs = sig(20)
    val version = bytes[8].toInt()
    private val tags: Map<String, Pair<Int, Int>> =
        (0 until u32(128)).associate { i -> sig(132 + 12 * i) to (u32(136 + 12 * i) to u32(140 + 12 * i)) }

    fun has(tag: String) = tag in tags
    fun tag(tag: String): Tag = tags[tag]?.let { (off, size) -> Tag(bytes, off, size) } ?: error("No $tag tag")

    private fun sig(at: Int) = bytes.decodeToString(at, at + 4).trim()
    private fun u32(at: Int) = Tag(bytes, 0, bytes.size).u32(at)

    companion object { fun load(path: String) = Profile(File(path).readBytes()) }
}

class Tag(private val b: ByteArray, val offset: Int, val size: Int) {
    val type get() = b.decodeToString(offset, offset + 4)
    fun u8(at: Int) = b[offset + at].toInt() and 0xff
    fun u16(at: Int) = (u8(at) shl 8) or u8(at + 1)
    fun u32(at: Int) = (u16(at) shl 16) or u16(at + 2)
    fun s15f16(at: Int) = u32(at) / 65536.0
    fun u8f8(at: Int) = u16(at) / 256.0
}

/** One-dimensional tone curve: `curv` (identity, gamma or table) or `para` (function types 0-4). */
class Curve(tag: Tag) {
    private val table: DoubleArray?
    private val params: DoubleArray?
    private val function: Int

    init {
        when (tag.type) {
            "curv" -> {
                val n = tag.u32(8)
                function = -1
                when (n) {
                    0 -> { table = null; params = doubleArrayOf(1.0) }
                    1 -> { table = null; params = doubleArrayOf(tag.u8f8(12)) }
                    else -> { table = DoubleArray(n) { tag.u16(12 + 2 * it) / 65535.0 }; params = null }
                }
            }
            "para" -> {
                function = tag.u16(8)
                val count = intArrayOf(1, 3, 4, 5, 7)[function]
                params = DoubleArray(count) { tag.s15f16(12 + 4 * it) }
                table = null
            }
            else -> error("Unsupported curve type ${tag.type}")
        }
    }

    fun eval(x: Double): Double {
        table?.let { return interpolate(it, x) }
        val p = params!!
        return when (function) {
            -1, 0 -> x.coerceAtLeast(0.0).pow(p[0])
            1 -> if (x >= -p[2] / p[1]) (p[1] * x + p[2]).pow(p[0]) else 0.0
            2 -> if (x >= -p[2] / p[1]) (p[1] * x + p[2]).pow(p[0]) + p[3] else p[3]
            3 -> if (x >= p[4]) (p[1] * x + p[2]).pow(p[0]) else p[3] * x
            4 -> if (x >= p[4]) (p[1] * x + p[2]).pow(p[0]) + p[5] else p[3] * x + p[6]
            else -> error("para $function")
        }
    }

    /** Inverse by bisection: curves in profiles are monotonic. */
    fun inverse(y: Double): Double {
        var lo = 0.0
        var hi = 1.0
        repeat(60) { val mid = (lo + hi) / 2; if (eval(mid) < y) lo = mid else hi = mid }
        return (lo + hi) / 2
    }

    private fun interpolate(t: DoubleArray, x: Double): Double {
        val pos = x.coerceIn(0.0, 1.0) * (t.size - 1)
        val i = pos.toInt().coerceAtMost(t.size - 2)
        return t[i] + (t[i + 1] - t[i]) * (pos - i)
    }
}

/** `lut16Type` (mft2) or `lut8Type` (mft1): matrix (XYZ input only), input curves, CLUT, output curves. */
class Lut(tag: Tag, private val pcsInputIsXyz: Boolean, private val labInput: Boolean = false) {
    private val bits16 = tag.type == "mft2"
    val inputs = tag.u8(8)
    val outputs = tag.u8(9)
    private val grid = tag.u8(10)
    private val matrix = DoubleArray(9) { tag.s15f16(12 + 4 * it) }
    private val inEntries = if (bits16) tag.u16(48) else 256
    private val outEntries = if (bits16) tag.u16(50) else 256
    private val inTables: Array<DoubleArray>
    private val clut: DoubleArray
    private val outTables: Array<DoubleArray>

    init {
        var p = if (bits16) 52 else 48
        fun next(): Double = if (bits16) tag.u16(p).toDouble() / 65535.0.also { p += 2 } else (tag.u8(p) / 255.0).also { p += 1 }
        inTables = Array(inputs) { DoubleArray(inEntries) { next() } }
        val points = (0 until inputs).fold(1) { acc, _ -> acc * grid }
        clut = DoubleArray(points * outputs) { next() }
        outTables = Array(outputs) { DoubleArray(outEntries) { next() } }
    }

    fun eval(input: DoubleArray): DoubleArray {
        var x = input
        if (pcsInputIsXyz && inputs == 3) x = DoubleArray(3) { r -> (0 until 3).sumOf { c -> matrix[r * 3 + c] * input[c] } }
        val curved = DoubleArray(inputs) { table(inTables[it], x[it]) }
        val mid = when (inputs) {
            // Like Little CMS: trilinear for Lab input (tetrahedral splits behave badly in Lab), tetrahedral otherwise.
            3 -> if (labInput) multilinear(curved) else tetrahedral(curved, 0)
            4 -> fourInputs(curved)
            else -> multilinear(curved)
        }
        return DoubleArray(outputs) { table(outTables[it], mid[it]) }
    }

    /** Tetrahedral interpolation over the 3-D CLUT starting at [baseIndex] (the slice for 4-input LUTs). */
    private fun tetrahedral(x: DoubleArray, baseIndex: Int, offset: Int = x.size - 3): DoubleArray {
        val b = IntArray(3)
        val r = DoubleArray(3)
        for (d in 0 until 3) {
            val pos = x[offset + d].coerceIn(0.0, 1.0) * (grid - 1)
            b[d] = pos.toInt().coerceAtMost(grid - 2)
            r[d] = pos - b[d]
        }
        fun at(dx: Int, dy: Int, dz: Int, o: Int) = clut[(baseIndex + ((b[0] + dx) * grid + b[1] + dy) * grid + b[2] + dz) * outputs + o]
        val (rx, ry, rz) = Triple(r[0], r[1], r[2])
        return DoubleArray(outputs) { o ->
            val c000 = at(0, 0, 0, o)
            val c111 = at(1, 1, 1, o)
            val (c1, c2, c3) = when {
                rx >= ry && ry >= rz -> Triple(at(1, 0, 0, o) - c000, at(1, 1, 0, o) - at(1, 0, 0, o), c111 - at(1, 1, 0, o))
                rx >= rz && rz >= ry -> Triple(at(1, 0, 0, o) - c000, c111 - at(1, 0, 1, o), at(1, 0, 1, o) - at(1, 0, 0, o))
                rz >= rx && rx >= ry -> Triple(at(1, 0, 1, o) - at(0, 0, 1, o), c111 - at(1, 0, 1, o), at(0, 0, 1, o) - c000)
                ry >= rx && rx >= rz -> Triple(at(1, 1, 0, o) - at(0, 1, 0, o), at(0, 1, 0, o) - c000, c111 - at(1, 1, 0, o))
                ry >= rz && rz >= rx -> Triple(c111 - at(0, 1, 1, o), at(0, 1, 0, o) - c000, at(0, 1, 1, o) - at(0, 1, 0, o))
                else -> Triple(c111 - at(0, 1, 1, o), at(0, 1, 1, o) - at(0, 0, 1, o), at(0, 0, 1, o) - c000)
            }
            c000 + c1 * rx + c2 * ry + c3 * rz
        }
    }

    /** Little CMS's 4-input scheme: tetrahedral in the last three inputs at two slices of the first, then linear. */
    private fun fourInputs(x: DoubleArray): DoubleArray {
        val pos = x[0].coerceIn(0.0, 1.0) * (grid - 1)
        val k = pos.toInt().coerceAtMost(grid - 2)
        val f = pos - k
        val slice = grid * grid * grid
        val lo = tetrahedral(x, k * slice, 1)
        val hi = tetrahedral(x, (k + 1) * slice, 1)
        return DoubleArray(outputs) { lo[it] + (hi[it] - lo[it]) * f }
    }

    /** N-dimensional multilinear interpolation over the CLUT. */
    private fun multilinear(x: DoubleArray): DoubleArray {
        val base = IntArray(inputs)
        val frac = DoubleArray(inputs)
        for (d in 0 until inputs) {
            val pos = x[d].coerceIn(0.0, 1.0) * (grid - 1)
            base[d] = pos.toInt().coerceAtMost(grid - 2)
            frac[d] = pos - base[d]
        }
        val out = DoubleArray(outputs)
        for (corner in 0 until (1 shl inputs)) {
            var weight = 1.0
            var index = 0
            for (d in 0 until inputs) {
                val bit = (corner shr (inputs - 1 - d)) and 1
                weight *= if (bit == 1) frac[d] else 1 - frac[d]
                index = index * grid + base[d] + bit
            }
            if (weight == 0.0) continue
            for (o in 0 until outputs) out[o] += weight * clut[index * outputs + o]
        }
        return out
    }

    private fun table(t: DoubleArray, x: Double): Double {
        val pos = x.coerceIn(0.0, 1.0) * (t.size - 1)
        val i = pos.toInt().coerceAtMost(t.size - 2)
        return t[i] + (t[i + 1] - t[i]) * (pos - i)
    }

    /** Lab encoding inside lut16/lut8 (legacy v2: L 0..100 and a/b -128..127 map to 0..0xFF00 in 16 bits). */
    fun decodeLab(v: DoubleArray): DoubleArray =
        if (bits16) doubleArrayOf(v[0] * 65535 / 65280 * 100, v[1] * 65535 / 256 - 128, v[2] * 65535 / 256 - 128)
        else doubleArrayOf(v[0] * 100, v[1] * 255 - 128, v[2] * 255 - 128)

    fun encodeLab(lab: DoubleArray): DoubleArray =
        if (bits16) doubleArrayOf(lab[0] / 100 * 65280 / 65535, (lab[1] + 128) * 256 / 65535, (lab[2] + 128) * 256 / 65535)
        else doubleArrayOf(lab[0] / 100, (lab[1] + 128) / 255, (lab[2] + 128) / 255)
}
