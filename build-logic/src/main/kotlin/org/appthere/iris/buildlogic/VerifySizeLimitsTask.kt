package org.appthere.iris.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails when a Kotlin file is longer than docs/coding-standards.md section 2 allows; reports every offender. */
abstract class VerifySizeLimitsTask : DefaultTask() {
    /** The module's Kotlin sources under `src/`. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val moduleDirectory: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val offenders = findOversizedFiles(moduleDirectory.get().asFile)
        report.get().asFile.writeText(offenders.joinToString("\n", postfix = "\n"))
        if (offenders.isNotEmpty()) {
            throw GradleException("Files over the size limit (split them):\n" + offenders.joinToString("\n") { "  - $it" })
        }
    }
}
