package org.appthere.iris.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FakeClockTest {
    private val start = Instant.parse("2026-10-05T12:00:00Z")

    @Test
    fun timeStandsStillUntilAdvanced() {
        val clock = FakeClock(start)

        assertEquals(start, clock.now())
        assertEquals(Duration.ZERO, clock.monotonic())
        assertEquals(start, clock.now())
    }

    @Test
    fun advanceMovesWallAndMonotonicTimeTogether() {
        val clock = FakeClock(start)

        clock.advance(750.milliseconds)
        clock.advance(1.seconds)

        assertEquals(start + 1750.milliseconds, clock.now())
        assertEquals(1750.milliseconds, clock.monotonic())
    }

    @Test
    fun aWallClockJumpLeavesMonotonicTimeAlone() {
        val clock = FakeClock(start)
        clock.advance(2.seconds)

        clock.setWallTime(start - 3600.seconds)

        assertEquals(start - 3600.seconds, clock.now())
        assertEquals(2.seconds, clock.monotonic())
    }

    @Test
    fun timeCannotRunBackwards() {
        assertFailsWith<IllegalArgumentException> { FakeClock(start).advance((-1).milliseconds) }
    }

    @Test
    fun theDefaultStartIsFixed() {
        assertEquals(FakeClock().now(), FakeClock().now())
    }
}
