package org.appthere.iris.core

import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.TimeSource

/** The real [Clock]. Apps create the engine with it; tests use a fake. */
public object SystemClock : Clock {
    private val origin = TimeSource.Monotonic.markNow()

    override fun now(): Instant =
        kotlin.time.Clock.System
            .now()

    override fun monotonic(): Duration = origin.elapsedNow()
}
