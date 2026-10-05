package org.appthere.iris.core

import kotlin.math.max
import kotlin.math.min

/** An axis-aligned rectangle of pixels or tiles, half-open: it contains [left, right) x [top, bottom). */
public data class IntRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(left <= right && top <= bottom) { "IntRect edges are inverted: $this" }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top

    /** Long, because a large canvas has more pixels than an Int holds. */
    val area: Long get() = width.toLong() * height.toLong()
    val isEmpty: Boolean get() = width == 0 || height == 0

    public fun contains(
        x: Int,
        y: Int,
    ): Boolean = x >= left && x < right && y >= top && y < bottom

    /** The overlap with [other], or null when they do not overlap (touching edges do not). */
    public fun intersect(other: IntRect): IntRect? {
        val l = max(left, other.left)
        val t = max(top, other.top)
        val r = min(right, other.right)
        val b = min(bottom, other.bottom)
        return if (l < r && t < b) IntRect(l, t, r, b) else null
    }

    /** The smallest rectangle containing both. */
    public fun union(other: IntRect): IntRect =
        IntRect(min(left, other.left), min(top, other.top), max(right, other.right), max(bottom, other.bottom))
}
