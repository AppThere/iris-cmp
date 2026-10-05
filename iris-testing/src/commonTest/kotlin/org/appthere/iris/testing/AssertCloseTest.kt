package org.appthere.iris.testing

import kotlin.math.nextUp
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class AssertCloseTest {
    @Test
    fun valuesWithinTheAbsoluteToleranceAreClose() {
        assertClose(1.0, 1.0 + 1e-7, absolute = 1e-6)
        assertClose(0.5f, 0.5001f, absolute = 1e-3f)
    }

    @Test
    fun valuesWithinTheUlpToleranceAreClose() {
        val next = 1.0.nextUp().nextUp()

        assertClose(1.0, next, ulps = 2)
        assertFailsWith<AssertionError> { assertClose(1.0, next, ulps = 1) }
    }

    @Test
    fun ulpsCountAcrossZero() {
        assertClose(0.0, -0.0)
        assertClose(Double.MIN_VALUE, -Double.MIN_VALUE, ulps = 2)
        assertFailsWith<AssertionError> { assertClose(Double.MIN_VALUE, -Double.MIN_VALUE, ulps = 1) }
    }

    @Test
    fun withNoToleranceOnlyEqualValuesAreClose() {
        assertClose(0.25, 0.25)
        assertFailsWith<AssertionError> { assertClose(0.25, 0.25.nextUp()) }
    }

    @Test
    fun nanMatchesOnlyNanAndInfinitiesMatchOnlyThemselves() {
        assertClose(Double.NaN, Double.NaN)
        assertClose(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        assertFailsWith<AssertionError> { assertClose(Double.NaN, 1.0, absolute = 10.0) }
        assertFailsWith<AssertionError> { assertClose(Double.POSITIVE_INFINITY, Double.MAX_VALUE, ulps = 10) }
        assertFailsWith<AssertionError> { assertClose(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, absolute = 1f) }
    }

    @Test
    fun theFailureMessageNamesTheValuesAndTheTolerance() {
        val error = assertFailsWith<AssertionError> { assertClose(1.0, 1.5, absolute = 0.1, message = "opacity") }

        assertContains(error.message.orEmpty(), "opacity")
        assertContains(error.message.orEmpty(), "expected 1.0")
        assertContains(error.message.orEmpty(), "was 1.5")
        assertContains(error.message.orEmpty(), "absolute 0.1")
    }
}
