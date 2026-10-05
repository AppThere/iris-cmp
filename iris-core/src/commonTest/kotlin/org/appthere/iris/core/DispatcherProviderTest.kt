package org.appthere.iris.core

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DispatcherProviderTest {
    private val provider: DispatcherProvider = DefaultDispatcherProvider(renderParallelism = 2)

    @Test
    fun workRunsOnEveryDispatcher() =
        runTest {
            val results =
                listOf(provider.compute, provider.io, provider.render).map { dispatcher ->
                    withContext(dispatcher) { 6 * 7 }
                }

            assertEquals(listOf(42, 42, 42), results)
        }

    @Test
    fun renderParallelismMustBePositive() {
        assertFailsWith<IllegalArgumentException> { DefaultDispatcherProvider(renderParallelism = 0) }
    }
}
