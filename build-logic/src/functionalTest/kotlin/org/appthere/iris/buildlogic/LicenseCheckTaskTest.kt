package org.appthere.iris.buildlogic

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LicenseCheckTaskTest {
    @TempDir
    lateinit var dir: File

    private val module = ":${FixtureProject.MODULE}"

    @Test
    fun `a GPL dependency in shipped code fails licenseCheck`() {
        val project = fixtureWith("implementation(\"fixture:gpl-lib:1.0\")")

        val result = project.runner("$module:licenseCheck").buildAndFail()

        assertContains(result.output, "fixture:gpl-lib:1.0 is licensed under 'GNU General Public License v3.0', which is not allowed in shipped code")
    }

    @Test
    fun `Apache in shipped code and EPL from a parent POM in tests pass, with the configuration cache`() {
        val project =
            fixtureWith(
                "implementation(\"fixture:apache-lib:1.0\")",
                "testImplementation(\"fixture:epl-lib:1.0\")",
            )

        val result = project.runner("$module:licenseCheck", "--configuration-cache").build()

        assertEquals(TaskOutcome.SUCCESS, result.task("$module:licenseCheck")?.outcome)
    }

    private fun fixtureWith(vararg dependencies: String): FixtureProject {
        val repo = dir.resolve("repo")
        publish(repo, "gpl-lib", license("GNU General Public License v3.0"))
        publish(repo, "apache-lib", license("Apache License, Version 2.0"))
        publish(repo, "epl-parent", license("Eclipse Public License v2.0"), packaging = "pom")
        publish(repo, "epl-lib", "", parent = "epl-parent")
        return FixtureProject.jvmApp(dir).withBuildScript(
            """
            repositories { maven { url = uri("${repo.invariantSeparatorsPath}") } }
            dependencies {
            ${dependencies.joinToString("\n") { "    $it" }}
            }
            """,
        )
    }

    private fun license(name: String) = "<licenses><license><name>$name</name></license></licenses>"

    private fun publish(
        repo: File,
        artifact: String,
        licenses: String,
        parent: String? = null,
        packaging: String = "jar",
    ) {
        val dir = repo.resolve("fixture/$artifact/1.0")
        dir.mkdirs()
        val parentXml = parent?.let { "<parent><groupId>fixture</groupId><artifactId>$it</artifactId><version>1.0</version></parent>" }
        dir.resolve("$artifact-1.0.pom").writeText(
            """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              ${parentXml.orEmpty()}
              <groupId>fixture</groupId>
              <artifactId>$artifact</artifactId>
              <version>1.0</version>
              <packaging>$packaging</packaging>
              $licenses
            </project>
            """.trimIndent(),
        )
        if (packaging == "jar") ZipOutputStream(dir.resolve("$artifact-1.0.jar").outputStream()).close()
    }
}
