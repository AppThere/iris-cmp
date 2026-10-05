package org.appthere.iris.core

/** Discards everything; the default where no platform logger is injected. */
public object NoOpLogger : Logger {
    override fun isEnabled(level: LogLevel): Boolean = false

    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable?,
    ): Unit = Unit
}
