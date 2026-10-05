package org.appthere.iris.core

import kotlin.time.Duration
import kotlin.time.Instant

/** Time, injected so engine code never reads the system clock directly (coding standards section 8). */
public interface Clock {
    /**
     * Wall-clock time, for timestamps that people see or that are stored.
     * It can jump when the system clock changes.
     */
    public fun now(): Instant

    /** Time since a fixed origin; never goes backwards. For durations, timeouts and coalescing windows. */
    public fun monotonic(): Duration
}
