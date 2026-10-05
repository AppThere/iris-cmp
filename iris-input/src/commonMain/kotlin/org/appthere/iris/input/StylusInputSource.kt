package org.appthere.iris.input

import kotlinx.coroutines.flow.Flow

/** A platform's stylus and pointer input (docs/architecture.md section 14). */
public interface StylusInputSource {
    public val capabilities: StylusCapabilities

    /** Hot and buffered; never blocks the producer. Events before a collector subscribes are not replayed. */
    public val events: Flow<RawPenEvent>

    /** Predicted samples up to [horizonMillis] ahead, or empty where the platform does not predict. */
    public fun predicted(horizonMillis: Int): List<RawPenEvent>
}
