package s7

import kotlin.math.cbrt
import kotlin.math.sqrt

/** Matrix/TRC RGB profile: device RGB <-> PCS XYZ (D50). */
class MatrixShaper(p: Profile) {
    private val curves = listOf("rTRC", "gTRC", "bTRC").map { Curve(p.tag(it)) }
    private val m = listOf("rXYZ", "gXYZ", "bXYZ").map { t -> p.tag(t).let { doubleArrayOf(it.s15f16(8), it.s15f16(12), it.s15f16(16)) } }
    private val inv = invert3(DoubleArray(9) { i -> m[i % 3][i / 3] })

    fun toXyz(rgb: DoubleArray): DoubleArray {
        val lin = DoubleArray(3) { curves[it].eval(rgb[it]) }
        return DoubleArray(3) { r -> (0 until 3).sumOf { c -> m[c][r] * lin[c] } }
    }

    fun fromXyz(xyz: DoubleArray): DoubleArray {
        val lin = DoubleArray(3) { r -> (0 until 3).sumOf { c -> inv[r * 3 + c] * xyz[c] } }
        return DoubleArray(3) { curves[it].inverse(lin[it].coerceIn(0.0, 1.0)) }
    }
}

private val D50 = doubleArrayOf(0.9642, 1.0, 0.8249)

fun xyzToLab(xyz: DoubleArray): DoubleArray {
    fun f(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
    val (fx, fy, fz) = List(3) { f(xyz[it] / D50[it]) }
    return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
}

fun deltaE76(a: DoubleArray, b: DoubleArray) = sqrt((0 until 3).sumOf { (a[it] - b[it]) * (a[it] - b[it]) })

fun invert3(m: DoubleArray): DoubleArray {
    val (a, b, c, d, e) = m.take(5)
    val f = m[5]; val g = m[6]; val h = m[7]; val i = m[8]
    val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    return doubleArrayOf(e * i - f * h, c * h - b * i, b * f - c * e, f * g - d * i, a * i - c * g, c * d - a * f, d * h - e * g, b * g - a * h, a * e - b * d).map { it / det }.toDoubleArray()
}
