package org.appthere.iris.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/** Stages golden candidates matching `-Pname` in `testdata/golden-pending/` for review (docs/testing-tdd.md section 4). */
@DisableCachingByDefault(because = "Copies files a human reviews; always runs")
abstract class GoldenUpdateTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val goldenName: Property<String>

    /** Module name to its `build/golden-candidates/` directory, where the golden harness writes mismatching output. */
    @get:Input
    abstract val candidateDirectories: MapProperty<String, String>

    @get:Internal
    abstract val pendingDirectory: DirectoryProperty

    @TaskAction
    fun update() {
        val name = goldenName.orNull ?: throw GradleException("goldenUpdate needs -Pname=<name> (or <module>/<name>; * matches anything)")
        val candidates = candidateDirectories.get().mapValues { File(it.value) }
        val promoted = promoteGoldenCandidates(candidates, pendingDirectory.get().asFile, name)
        if (promoted.isEmpty()) {
            throw GradleException("No golden candidates match '$name'. Run the failing golden test first; it writes build/golden-candidates/.")
        }
        logger.lifecycle("Staged for review in testdata/golden-pending/:\n" + promoted.joinToString("\n") { "  $it" })
    }
}

/** Lists pending golden adds and changes against `testdata/golden/`; reports only, never fails. */
@DisableCachingByDefault(because = "Prints a report for a human")
abstract class GoldenReviewTask : DefaultTask() {
    @get:Internal
    abstract val goldenDirectory: DirectoryProperty

    @get:Internal
    abstract val pendingDirectory: DirectoryProperty

    @TaskAction
    fun review() {
        val changes = reviewGoldens(goldenDirectory.get().asFile, pendingDirectory.get().asFile)
        if (changes.isEmpty()) {
            logger.lifecycle("No pending golden changes.")
            return
        }
        logger.lifecycle("Pending golden changes (testdata/golden-pending/ against testdata/golden/):")
        changes.forEach { logger.lifecycle("  " + it.kind.name.lowercase().padEnd(LABEL_WIDTH) + it.path) }
        if (changes.any { it.kind == GoldenChangeKind.SAME }) {
            logger.lifecycle("'same' files are identical to their golden and can be deleted from testdata/golden-pending/.")
        }
    }

    private companion object {
        const val LABEL_WIDTH = 8
    }
}
