package org.appthere.iris.core

/**
 * Logging, injected; platforms supply implementations (docs/architecture.md section 16).
 * Messages are for developers and never contain document content or user file paths.
 * Prefer the [debug], [info], [warn] and [error] helpers: they build the message only when the level is enabled.
 */
public interface Logger {
    public fun isEnabled(level: LogLevel): Boolean

    public fun log(
        level: LogLevel,
        tag: String,
        message: String,
        error: Throwable? = null,
    )
}

public inline fun Logger.debug(
    tag: String,
    error: Throwable? = null,
    message: () -> String,
) {
    if (isEnabled(LogLevel.DEBUG)) log(LogLevel.DEBUG, tag, message(), error)
}

public inline fun Logger.info(
    tag: String,
    error: Throwable? = null,
    message: () -> String,
) {
    if (isEnabled(LogLevel.INFO)) log(LogLevel.INFO, tag, message(), error)
}

public inline fun Logger.warn(
    tag: String,
    error: Throwable? = null,
    message: () -> String,
) {
    if (isEnabled(LogLevel.WARN)) log(LogLevel.WARN, tag, message(), error)
}

public inline fun Logger.error(
    tag: String,
    error: Throwable? = null,
    message: () -> String,
) {
    if (isEnabled(LogLevel.ERROR)) log(LogLevel.ERROR, tag, message(), error)
}
