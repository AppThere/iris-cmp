package org.appthere.iris.cli

import java.util.Properties
import kotlin.system.exitProcess

private const val EXIT_OK = 0
private const val EXIT_USAGE = 2

private const val USAGE = "Usage: iris --version | --help\n"

fun main(args: Array<String>) {
    exitProcess(runCli(args.toList(), System.out, System.err))
}

/** Runs the CLI with [args], writing to [out] and [err]; returns the process exit code. */
internal fun runCli(
    args: List<String>,
    out: Appendable,
    err: Appendable,
): Int =
    when (args) {
        listOf("--version") -> {
            out.append("iris ${irisVersion()}\n")
            EXIT_OK
        }

        listOf("--help") -> {
            out.append(USAGE)
            EXIT_OK
        }

        else -> {
            err.append(USAGE)
            EXIT_USAGE
        }
    }

/** The version the build wrote into `version.properties`. */
private fun irisVersion(): String {
    val resource =
        checkNotNull(object {}.javaClass.getResourceAsStream("version.properties")) { "version.properties is missing" }
    return resource.use { Properties().apply { load(it) }.getProperty("version") }
}
