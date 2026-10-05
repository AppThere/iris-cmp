package org.appthere.iris.testing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.appthere.iris.input.RawPenEvent
import org.appthere.iris.input.StylusCapabilities
import org.appthere.iris.input.StylusInputSource

/**
 * A [StylusInputSource] that plays a fixed [script] when the test says so. Like a real source it is hot:
 * start collecting [events] before calling [playNext] or [playAll].
 */
public class ScriptedStylusSource(
    override val capabilities: StylusCapabilities,
    private val script: List<RawPenEvent>,
    private val predictions: (horizonMillis: Int) -> List<RawPenEvent> = { emptyList() },
) : StylusInputSource {
    private val flow = MutableSharedFlow<RawPenEvent>()
    private var played = 0

    override val events: SharedFlow<RawPenEvent> = flow.asSharedFlow()

    /** Events of the script not played yet. */
    public val remaining: Int get() = script.size - played

    /** Emits the next [count] scripted events, suspending until every collector has them. */
    public suspend fun playNext(count: Int = 1) {
        require(count in 0..remaining) { "Cannot play $count events; $remaining remain" }
        repeat(count) { flow.emit(script[played++]) }
    }

    public suspend fun playAll() {
        playNext(remaining)
    }

    override fun predicted(horizonMillis: Int): List<RawPenEvent> = predictions(horizonMillis)
}
