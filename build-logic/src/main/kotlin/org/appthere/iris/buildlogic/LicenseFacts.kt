package org.appthere.iris.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

/** Every external component a module resolves for shipping or tests, with its scope and POM licenses. */
internal class LicenseFacts(
    /** `group:module:version` to `SHIPPED` or `TEST`; shipped wins when a component is in both. */
    val scopes: Map<String, String>,
    /** `group:module:version` to its licenses as `name|url`, inherited from parent POMs when absent. */
    val licenses: Map<String, List<String>>,
)

/** Resolves the configurations [licenseScopeOf] selects and reads each component's POM (and parent POMs). */
internal fun collectLicenseFacts(project: Project): LicenseFacts {
    val scopes = sortedMapOf<String, LicenseScope>()
    project.configurations.filter { it.isCanBeResolved }.forEach { configuration ->
        val scope = licenseScopeOf(configuration.name) ?: return@forEach
        configuration.incoming.resolutionResult.allComponents
            .mapNotNull { it.id as? ModuleComponentIdentifier }
            .forEach { id ->
                val key = "${id.group}:${id.module}:${id.version}"
                scopes[key] = if (scopes[key] == LicenseScope.SHIPPED) LicenseScope.SHIPPED else scope
            }
    }
    val poms = PomCache(project)
    return LicenseFacts(
        scopes = scopes.mapValues { it.value.name },
        licenses = scopes.keys.associateWith { coordinate -> licensesWithParents(coordinate, poms::get).map { "${it.name}|${it.url}" } },
    )
}

/** Fetches POMs through the build's repositories; null when a POM cannot be found. */
private class PomCache(
    private val project: Project,
) {
    private val poms = mutableMapOf<String, Pom?>()

    fun get(coordinate: String): Pom? = poms.getOrPut(coordinate) { fetch(coordinate) }

    // An artifact resolution query fetches the POM file itself. A `@pom` dependency would still select a variant
    // from Gradle module metadata, which is ambiguous for Kotlin/Native klibs.
    private fun fetch(coordinate: String): Pom? {
        val (group, module, version) = coordinate.split(":")
        val result =
            project.dependencies
                .createArtifactResolutionQuery()
                .forModule(group, module, version)
                .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
                .execute()
        val pom =
            result.resolvedComponents
                .flatMap { it.getArtifacts(MavenPomArtifact::class.java) }
                .filterIsInstance<ResolvedArtifactResult>()
                .firstOrNull()
        return pom?.file?.readText()?.let(::parsePom)
    }
}
