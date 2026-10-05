package org.appthere.iris.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import org.appthere.iris.core.DispatcherProvider

/**
 * A [DispatcherProvider] whose dispatchers are all one [StandardTestDispatcher]: work runs only when the
 * test runs [scheduler], and delays take virtual time. Pass [dispatcher] to `runTest` to share its scheduler.
 */
public class FakeDispatcherProvider(
    public val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
) : DispatcherProvider {
    public val dispatcher: TestDispatcher = StandardTestDispatcher(scheduler, name = "iris-test")

    override val compute: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val render: CoroutineDispatcher get() = dispatcher
}
