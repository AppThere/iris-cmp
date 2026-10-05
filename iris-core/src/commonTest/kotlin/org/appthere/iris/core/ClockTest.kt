package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ClockTest {
    @Test
    fun monotonicTimeNeverGoesBackwards() {
        val readings = List(10_000) { SystemClock.monotonic() }

        readings.zipWithNext().forEach { (earlier, later) -> assertTrue(later >= earlier, "$later is before $earlier") }
    }

    @Test
    fun wallTimeMatchesTheSystemClock() {
        val before =
            kotlin.time.Clock.System
                .now()
        val now = SystemClock.now()
        val after =
            kotlin.time.Clock.System
                .now()

        assertTrue(now >= before - 1.seconds && now <= after + 1.seconds, "$now is not between $before and $after")
    }
}
