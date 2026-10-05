package org.appthere.iris.testing

import kotlin.math.abs
import kotlin.test.fail

/**
 * Asserts that [actual] is within [absolute] of [expected], or within [ulps] units in the last place.
 * With both tolerances zero, the values must be equal. NaN matches only NaN, and an infinity only itself.
 */
public fun assertClose(
    expected: Double,
    actual: Double,
    absolute: Double = 0.0,
    ulps: Int = 0,
    message: String? = null,
) {
    val distance = ulpDistance(orderedBits(expected), orderedBits(actual))
    val close =
        when {
            expected.isNaN() || actual.isNaN() -> expected.isNaN() && actual.isNaN()
            expected.isInfinite() || actual.isInfinite() -> expected == actual
            else -> abs(expected - actual) <= absolute || distance <= ulps.toULong()
        }
    if (!close) {
        fail(
            describe(message, "$expected", "$actual", "${abs(expected - actual)}", distance, "$absolute", ulps),
        )
    }
}

/** [assertClose] for Floats; ulps count Float steps. */
public fun assertClose(
    expected: Float,
    actual: Float,
    absolute: Float = 0f,
    ulps: Int = 0,
    message: String? = null,
) {
    val distance = ulpDistance(orderedBits(expected).toLong(), orderedBits(actual).toLong())
    val close =
        when {
            expected.isNaN() || actual.isNaN() -> expected.isNaN() && actual.isNaN()
            expected.isInfinite() || actual.isInfinite() -> expected == actual
            else -> abs(expected - actual) <= absolute || distance <= ulps.toULong()
        }
    if (!close) {
        fail(
            describe(message, "$expected", "$actual", "${abs(expected - actual)}", distance, "$absolute", ulps),
        )
    }
}

/** Maps a value's bits onto one ordered line, so -0.0 and 0.0 coincide and neighbours differ by one. */
private fun orderedBits(value: Double): Long {
    val bits = value.toRawBits()
    return if (bits < 0) Long.MIN_VALUE - bits else bits
}

private fun orderedBits(value: Float): Int {
    val bits = value.toRawBits()
    return if (bits < 0) Int.MIN_VALUE - bits else bits
}

/** Unsigned, so the distance between the most negative and most positive values does not overflow. */
private fun ulpDistance(
    a: Long,
    b: Long,
): ULong = if (a >= b) a.toULong() - b.toULong() else b.toULong() - a.toULong()

private fun describe(
    message: String?,
    expected: String,
    actual: String,
    difference: String,
    ulpDistance: ULong,
    absolute: String,
    ulps: Int,
): String {
    val prefix = message?.let { "$it: " }.orEmpty()
    return "${prefix}expected $expected but was $actual " +
        "(difference $difference, $ulpDistance ulps; allowed absolute $absolute or $ulps ulps)"
}
