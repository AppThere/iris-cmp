package org.appthere.iris.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Fails when a shipped or test dependency has a license outside the D-020 allow-list; reports every offender. */
abstract class LicenseCheckTask : DefaultTask() {
    /** `group:module:version` to `SHIPPED` or `TEST`. */
    @get:Input
    abstract val componentScopes: MapProperty<String, String>

    /** `group:module:version` to its POM licenses as `name|url`. */
    @get:Input
    abstract val componentLicenses: MapProperty<String, List<String>>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val licenses = componentLicenses.get()
        val violations =
            componentScopes.get().mapNotNull { (component, scope) ->
                val poms = licenses[component].orEmpty().map { it.substringBefore('|') to it.substringAfter('|') }
                licenseViolation(component, poms.map { (name, url) -> PomLicense(name, url) }, LicenseScope.valueOf(scope))
            }
        report.get().asFile.writeText(
            componentScopes.get().entries.joinToString("\n", postfix = "\n") { (c, s) -> "$c $s ${licenses[c].orEmpty()}" },
        )
        if (violations.isNotEmpty()) {
            throw GradleException("Dependencies with licenses outside the allow-list (D-020):\n" + violations.joinToString("\n") { "  - $it" })
        }
    }
}
