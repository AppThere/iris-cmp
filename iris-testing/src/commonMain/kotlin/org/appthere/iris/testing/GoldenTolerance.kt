package org.appthere.iris.testing

/** How far a golden comparison may drift: per sample ([maxChannelDelta]) and on average ([maxMeanDelta]). */
public data class GoldenTolerance(
    val maxChannelDelta: Float,
    val maxMeanDelta: Float,
) {
    init {
        require(maxChannelDelta >= 0f && maxMeanDelta >= 0f) { "Tolerances must not be negative: $this" }
    }

    public companion object {
        /** Bit-exact, for CPU goldens that must match on every target. */
        public val EXACT: GoldenTolerance = GoldenTolerance(0f, 0f)
    }
}
