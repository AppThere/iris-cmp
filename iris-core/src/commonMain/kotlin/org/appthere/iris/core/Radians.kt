package org.appthere.iris.core

import kotlin.jvm.JvmInline
import kotlin.math.PI

/** An angle in radians; positive turns from +x toward +y (clockwise on screen, where y points down). */
@JvmInline
public value class Radians(
    public val value: Double,
) {
    public companion object {
        public fun fromDegrees(degrees: Double): Radians = Radians(degrees * PI / HALF_TURN_DEGREES)

        private const val HALF_TURN_DEGREES = 180.0
    }
}
