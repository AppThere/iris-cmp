package org.appthere.iris.testing

import org.appthere.iris.core.LogLevel
import org.appthere.iris.core.debug
import org.appthere.iris.core.error
import org.appthere.iris.core.info
import org.appthere.iris.core.warn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RecordingLoggerTest {
    @Test
    fun itRecordsEveryEntryInOrder() {
        val logger = RecordingLogger()
        val failure = IllegalStateException("disk full")

        logger.info("io") { "saving" }
        logger.error("io", failure) { "save failed" }

        assertEquals(
            listOf(
                RecordingLogger.Entry(LogLevel.INFO, "io", "saving", null),
                RecordingLogger.Entry(LogLevel.ERROR, "io", "save failed", failure),
            ),
            logger.entries,
        )
        assertSame(failure, logger.entries.last().error)
    }

    @Test
    fun levelsBelowTheMinimumAreNeitherBuiltNorRecorded() {
        val logger = RecordingLogger(minimum = LogLevel.WARN)
        var built = 0

        logger.debug("render") {
            built++
            "tile"
        }
        logger.warn("render") { "slow frame" }

        assertEquals(0, built)
        assertEquals(listOf("slow frame"), logger.messages())
    }

    @Test
    fun messagesCanBeFilteredByLevelAndCleared() {
        val logger = RecordingLogger()
        logger.info("a") { "one" }
        logger.warn("b") { "two" }

        val warnings = logger.messages(LogLevel.WARN)
        logger.clear()

        assertEquals(listOf("two"), warnings)
        assertEquals(emptyList(), logger.entries)
    }
}
