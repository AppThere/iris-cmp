package org.appthere.iris.core

/**
 * A point in document space. Doubles, because geometry accumulates through nested transforms
 * (coding standards section 5).
 */
public data class Point(
    val x: Double,
    val y: Double,
) {
    public operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)

    public operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

    public companion object {
        public val ORIGIN: Point = Point(0.0, 0.0)
    }
}
