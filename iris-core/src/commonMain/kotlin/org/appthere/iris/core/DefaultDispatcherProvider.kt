package org.appthere.iris.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * The real dispatchers. [renderParallelism] bounds how many render workers run at once;
 * the render pool shares [compute]'s threads.
 */
public class DefaultDispatcherProvider(
    renderParallelism: Int,
    override val compute: CoroutineDispatcher = Dispatchers.Default,
    override val io: CoroutineDispatcher = Dispatchers.IO,
) : DispatcherProvider {
    init {
        require(renderParallelism > 0) { "renderParallelism must be positive, was $renderParallelism" }
    }

    override val render: CoroutineDispatcher = compute.limitedParallelism(renderParallelism, "iris-render")
}
