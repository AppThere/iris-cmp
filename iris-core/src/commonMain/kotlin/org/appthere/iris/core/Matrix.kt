package org.appthere.iris.core

import kotlin.math.cos
import kotlin.math.sin

/**
 * A 2D affine transform in SVG order (docs/file-format.md): x' = a x + c y + e, y' = b x + d y + f.
 * `(m * n).map(p) == m.map(n.map(p))`: the right operand applies first.
 */
public data class Matrix(
    val a: Double,
    val b: Double,
    val c: Double,
    val d: Double,
    val e: Double,
    val f: Double,
) {
    val determinant: Double get() = a * d - b * c

    public fun map(point: Point): Point = Point(a * point.x + c * point.y + e, b * point.x + d * point.y + f)

    /** The bounding box of [rect]'s four mapped corners. */
    public fun mapRect(rect: Rect): Rect {
        val corners =
            listOf(
                map(Point(rect.left, rect.top)),
                map(Point(rect.right, rect.top)),
                map(Point(rect.left, rect.bottom)),
                map(Point(rect.right, rect.bottom)),
            )
        return Rect(
            corners.minOf { it.x },
            corners.minOf { it.y },
            corners.maxOf { it.x },
            corners.maxOf { it.y },
        )
    }

    public operator fun times(other: Matrix): Matrix =
        Matrix(
            a = a * other.a + c * other.b,
            b = b * other.a + d * other.b,
            c = a * other.c + c * other.d,
            d = b * other.c + d * other.d,
            e = a * other.e + c * other.f + e,
            f = b * other.e + d * other.f + f,
        )

    /** The inverse transform, or null when this matrix is singular (determinant zero or not finite). */
    public fun inverse(): Matrix? {
        val det = determinant
        if (det == 0.0 || !det.isFinite()) return null
        return Matrix(
            a = d / det,
            b = -b / det,
            c = -c / det,
            d = a / det,
            e = (c * f - d * e) / det,
            f = (b * e - a * f) / det,
        )
    }

    public companion object {
        public val IDENTITY: Matrix = Matrix(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

        public fun translate(
            dx: Double,
            dy: Double,
        ): Matrix = Matrix(1.0, 0.0, 0.0, 1.0, dx, dy)

        public fun scale(
            sx: Double,
            sy: Double = sx,
        ): Matrix = Matrix(sx, 0.0, 0.0, sy, 0.0, 0.0)

        public fun rotate(angle: Radians): Matrix {
            val cos = cos(angle.value)
            val sin = sin(angle.value)
            return Matrix(cos, sin, -sin, cos, 0.0, 0.0)
        }
    }
}
