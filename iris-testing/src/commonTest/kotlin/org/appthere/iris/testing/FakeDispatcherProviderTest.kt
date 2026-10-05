package org.appthere.iris.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class FakeDispatcherProviderTest {
    @Test
    fun workRunsOnlyWhenTheSchedulerRuns() {
        val provider = FakeDispatcherProvider()
        val ran = mutableListOf<String>()

        listOf(
            "compute" to provider.compute,
            "io" to provider.io,
            "render" to provider.render,
        ).forEach { (name, dispatcher) ->
            CoroutineScope(dispatcher).launch { ran += name }
        }
        val before = ran.toList()
        provider.scheduler.runCurrent()

        assertEquals(emptyList(), before)
        assertEquals(listOf("compute", "io", "render"), ran)
    }

    @Test
    fun delaysUseVirtualTime() {
        val provider = FakeDispatcherProvider()
        var done = false

        CoroutineScope(provider.render).launch {
            delay(10.seconds)
            done = true
        }
        provider.scheduler.advanceTimeBy(9.seconds)
        val early = done
        provider.scheduler.advanceTimeBy(1.seconds)
        provider.scheduler.runCurrent()

        assertEquals(false, early)
        assertEquals(true, done)
    }

    @Test
    fun itSharesTheSchedulerOfRunTest() {
        val provider = FakeDispatcherProvider()

        runTest(provider.dispatcher) {
            withContext(provider.io) { delay(1.hours) }

            assertEquals(1.hours.inWholeMilliseconds, testScheduler.currentTime)
            assertSame(provider.scheduler, testScheduler)
        }
    }
}
