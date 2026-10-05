package org.appthere.iris.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** An axis-aligned rectangle in document space, half-open: it contains [left, right) x [top, bottom). */
public data class Rect(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
) {
    init {
        require(left <= right && top <= bottom) { "Rect edges are inverted or NaN: $this" }
    }

    val width: Double get() = right - left
    val height: Double get() = bottom - top
    val isEmpty: Boolean get() = width == 0.0 || height == 0.0
    val center: Point get() = Point((left + right) / 2, (top + bottom) / 2)

    public fun contains(point: Point): Boolean =
        point.x >= left && point.x < right && point.y >= top && point.y < bottom

    /** The overlap with [other], or null when they do not overlap (touching edges do not). */
    public fun intersect(other: Rect): Rect? {
        val l = max(left, other.left)
        val t = max(top, other.top)
        val r = min(right, other.right)
        val b = min(bottom, other.bottom)
        return if (l < r && t < b) Rect(l, t, r, b) else null
    }

    /** The smallest rectangle containing both. */
    public fun union(other: Rect): Rect =
        Rect(min(left, other.left), min(top, other.top), max(right, other.right), max(bottom, other.bottom))

    public fun translate(
        dx: Double,
        dy: Double,
    ): Rect = Rect(left + dx, top + dy, right + dx, bottom + dy)

    /** The smallest pixel rectangle covering every pixel this rectangle touches. */
    public fun roundOut(): IntRect =
        IntRect(floor(left).toInt(), floor(top).toInt(), ceil(right).toInt(), ceil(bottom).toInt())

    public companion object {
        public fun fromSize(
            x: Double,
            y: Double,
            width: Double,
            height: Double,
        ): Rect = Rect(x, y, x + width, y + height)
    }
}
