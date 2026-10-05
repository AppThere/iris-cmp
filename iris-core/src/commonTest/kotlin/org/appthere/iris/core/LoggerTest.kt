package org.appthere.iris.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class LoggerTest {
    private data class Entry(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val error: Throwable?,
    )

    private class Recording(
        private val minimum: LogLevel,
    ) : Logger {
        val entries = mutableListOf<Entry>()

        override fun isEnabled(level: LogLevel): Boolean = level >= minimum

        override fun log(
            level: LogLevel,
            tag: String,
            message: String,
            error: Throwable?,
        ) {
            entries += Entry(level, tag, message, error)
        }
    }

    @Test
    fun eachHelperLogsAtItsLevel() {
        val logger = Recording(LogLevel.DEBUG)
        val failure = IllegalStateException("boom")

        logger.debug("io") { "d" }
        logger.info("io") { "i" }
        logger.warn("io") { "w" }
        logger.error("io", failure) { "e" }

        assertEquals(
            listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR),
            logger.entries.map { it.level },
        )
        assertEquals(listOf("d", "i", "w", "e"), logger.entries.map { it.message })
        assertSame(failure, logger.entries.last().error)
    }

    @Test
    fun messagesForDisabledLevelsAreNeverBuilt() {
        val logger = Recording(LogLevel.WARN)
        var built = 0

        logger.debug("render") {
            built++
            "expensive"
        }
        logger.warn("render") {
            built++
            "kept"
        }

        assertEquals(1, built)
        assertEquals(listOf("kept"), logger.entries.map { it.message })
    }

    @Test
    fun theNoOpLoggerIsDisabledForEveryLevel() {
        LogLevel.entries.forEach { assertFalse(NoOpLogger.isEnabled(it), "$it") }
    }
}
