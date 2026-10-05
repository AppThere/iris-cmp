package org.appthere.iris.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.assertTrue

// Local helpers until iris-testing provides assertClose (A10).

internal fun assertNear(
    expected: Double,
    actual: Double,
    tolerance: Double = 1e-9,
    message: String = "",
) {
    val scale = max(1.0, max(abs(expected), abs(actual)))
    assertTrue(abs(expected - actual) <= tolerance * scale, "$message expected $expected but was $actual")
}

internal fun assertNear(
    expected: Point,
    actual: Point,
    tolerance: Double = 1e-9,
) {
    assertNear(expected.x, actual.x, tolerance, "x:")
    assertNear(expected.y, actual.y, tolerance, "y:")
}

internal fun assertNear(
    expected: Matrix,
    actual: Matrix,
    tolerance: Double = 1e-9,
) {
    listOf(
        expected.a to actual.a,
        expected.b to actual.b,
        expected.c to actual.c,
        expected.d to actual.d,
        expected.e to actual.e,
        expected.f to actual.f,
    ).forEach { (e, a) -> assertNear(e, a, tolerance, "$expected vs $actual:") }
}
