package org.appthere.iris.core

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Where engine coroutines run (coding standards section 7: no hard-coded `Dispatchers.*` in engine code).
 * Actors that need confinement derive their own with `limitedParallelism(1)`.
 */
public interface DispatcherProvider {
    /** CPU-bound work: filters, encoding, geometry. */
    public val compute: CoroutineDispatcher

    /** Blocking IO: files and streams. */
    public val io: CoroutineDispatcher

    /** The bounded pool render workers share (docs/architecture.md section 2). */
    public val render: CoroutineDispatcher
}
