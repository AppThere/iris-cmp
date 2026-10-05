package org.appthere.iris.input

import kotlin.jvm.JvmInline

/** Which optional axes an event carries. Absent axes are marked here, never zero-filled (product spec section 4). */
@JvmInline
public value class PenAxes(
    public val bits: Int,
) {
    public operator fun plus(other: PenAxes): PenAxes = PenAxes(bits or other.bits)

    public operator fun contains(axis: PenAxes): Boolean = bits and axis.bits == axis.bits

    public companion object {
        public val NONE: PenAxes = PenAxes(0)
        public val PRESSURE: PenAxes = PenAxes(1)
        public val TILT: PenAxes = PenAxes(1 shl 1)
        public val AZIMUTH: PenAxes = PenAxes(1 shl 2)
        public val TWIST: PenAxes = PenAxes(1 shl 3)
        public val HOVER_DISTANCE: PenAxes = PenAxes(1 shl 4)
        public val ALL: PenAxes = PenAxes((1 shl 5) - 1)
    }
}
