package org.appthere.iris.testing

import org.appthere.iris.core.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A [Clock] that moves only when a test says so. Not thread-safe; use it from the test's own thread. */
public class FakeClock(
    start: Instant = DEFAULT_START,
) : Clock {
    private var wall: Instant = start
    private var elapsed: Duration = Duration.ZERO

    override fun now(): Instant = wall

    override fun monotonic(): Duration = elapsed

    /** Moves wall and monotonic time forward by [by]. */
    public fun advance(by: Duration) {
        require(!by.isNegative()) { "Time cannot run backwards: $by" }
        wall += by
        elapsed += by
    }

    /** Jumps the wall clock (as a system clock change would) without moving monotonic time. */
    public fun setWallTime(instant: Instant) {
        wall = instant
    }

    public companion object {
        public val DEFAULT_START: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
