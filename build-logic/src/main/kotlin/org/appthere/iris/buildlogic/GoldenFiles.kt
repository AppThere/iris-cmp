package org.appthere.iris.buildlogic

import java.io.File

/** How a pending golden relates to `testdata/golden/`. */
internal enum class GoldenChangeKind { ADD, CHANGE, SAME }

/** One file in the pending area; [path] is `<module>/<name>.<ext>`. */
internal data class GoldenChange(
    val kind: GoldenChangeKind,
    val path: String,
)

/** Compares every file under [pending] with the file at the same path under [golden], byte for byte. */
internal fun reviewGoldens(
    golden: File,
    pending: File,
): List<GoldenChange> =
    relativeFiles(pending).map { path ->
        val current = golden.resolve(path)
        val kind =
            when {
                !current.isFile -> GoldenChangeKind.ADD
                current.readBytes().contentEquals(pending.resolve(path).readBytes()) -> GoldenChangeKind.SAME
                else -> GoldenChangeKind.CHANGE
            }
        GoldenChange(kind, path)
    }

/**
 * Copies the candidates (module name to its `build/golden-candidates/`) whose name matches [pattern]
 * to `[pending]/<module>/`. Returns the copied paths. Never writes to `testdata/golden/`.
 */
internal fun promoteGoldenCandidates(
    candidates: Map<String, File>,
    pending: File,
    pattern: String,
): List<String> =
    candidates.toSortedMap().flatMap { (module, dir) ->
        relativeFiles(dir).filter { matchesGoldenName(module, it, pattern) }.map { path ->
            dir.resolve(path).copyTo(pending.resolve("$module/$path"), overwrite = true)
            "$module/$path"
        }
    }

/** [pattern] is `<name>` or `<module>/<name>`, without extension; `*` matches any run of characters. */
internal fun matchesGoldenName(
    module: String,
    relativePath: String,
    pattern: String,
): Boolean {
    val name = relativePath.substringBeforeLast('.')
    val regex = Regex(pattern.split('*').joinToString(".*") { Regex.escape(it) })
    return regex.matches(name) || regex.matches("$module/$name")
}

private fun relativeFiles(dir: File): List<String> =
    dir
        .walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(dir).invariantSeparatorsPath }
        .sorted()
        .toList()
