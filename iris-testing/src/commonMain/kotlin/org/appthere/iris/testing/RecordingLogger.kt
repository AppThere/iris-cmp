package org.appthere.iris.testing

import org.appthere.iris.core.LogLevel
import org.appthere.iris.core.Logger

/** A [Logger] that keeps entries at or above [minimum] for assertions. Not thread-safe. */
public class RecordingLogger(
    private val minimum: LogLevel = LogLevel.DEBUG,
) : Logger {
    public data class Entry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val error: Throwable?,
    )

    private val recorded = mutableListOf<Entry>()

    /** A copy of everything recorded so far, oldest first. */
    public val entries: List<Entry> get() = recorded.toList()

    override fun isEnabled(level: LogLevel): Boolean = level >= minimum

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable?,
    ) {
        if (isEnabled(level)) recorded += Entry(level, tag, message, error)
    }

    /** Recorded messages, all of them or only those at [level]. */
    public fun messages(level: LogLevel? = null): List<String> =
        recorded.filter { level == null || it.level == level }.map { it.message }

    public fun clear() {
        recorded.clear()
    }
}
