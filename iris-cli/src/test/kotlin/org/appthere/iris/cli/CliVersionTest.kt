package org.appthere.iris.cli

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CliVersionTest {
    private val expectedVersion = requireNotNull(System.getProperty("iris.expectedVersion")) { "set by the test task" }

    @Test
    fun versionPrintsTheProjectVersion() {
        val out = StringBuilder()

        val exit = runCli(listOf("--version"), out, StringBuilder())

        assertEquals(0, exit)
        assertEquals("iris $expectedVersion\n", out.toString())
    }

    @Test
    fun helpPrintsUsage() {
        val out = StringBuilder()

        val exit = runCli(listOf("--help"), out, StringBuilder())

        assertEquals(0, exit)
        assertContains(out, "--version")
    }

    @Test
    fun unknownOrMissingArgumentsPrintUsageToStderrAndFail() {
        listOf(emptyList(), listOf("--frobnicate")).forEach { args ->
            val err = StringBuilder()

            val exit = runCli(args, StringBuilder(), err)

            assertEquals(2, exit, "$args")
            assertContains(err, "Usage: iris")
        }
    }
}
