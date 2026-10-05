package org.appthere.iris.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LicenseAllowListTest {
    @Test
    fun `an Apache-licensed artifact is accepted`() {
        val pom = parsePom(pom(license("The Apache Software License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt")))

        assertNull(licenseViolation("org.example:lib:1.0", pom.licenses, LicenseScope.SHIPPED))
    }

    @Test
    fun `a GPL artifact is rejected`() {
        val pom = parsePom(pom(license("GNU General Public License v3.0", "https://www.gnu.org/licenses/gpl-3.0.txt")))

        assertEquals(
            "org.example:gpl:1.0 is licensed under 'GNU General Public License v3.0', which is not allowed in shipped code",
            licenseViolation("org.example:gpl:1.0", pom.licenses, LicenseScope.SHIPPED),
        )
    }

    @Test
    fun `an artifact without a license is rejected`() {
        val pom = parsePom(pom(""))

        assertEquals(
            "org.example:bare:1.0 declares no license in its POM (or its parents)",
            licenseViolation("org.example:bare:1.0", pom.licenses, LicenseScope.SHIPPED),
        )
    }

    @Test
    fun `a dual-licensed artifact is accepted through its allowed license`() {
        val pom = parsePom(pom(license("GNU Lesser General Public License", "") + license("MIT License", "https://opensource.org/licenses/MIT")))

        assertNull(licenseViolation("org.example:dual:1.0", pom.licenses, LicenseScope.SHIPPED))
    }

    @Test
    fun `EPL is allowed in tests only`() {
        val epl = listOf(PomLicense("Eclipse Public License v2.0", "https://www.eclipse.org/legal/epl-v20.html"))

        assertNull(licenseViolation("org.junit:junit-bom:5.0", epl, LicenseScope.TEST))
        assertEquals(
            "org.junit:junit-bom:5.0 is licensed under EPL-2.0, which is not allowed in shipped code",
            licenseViolation("org.junit:junit-bom:5.0", epl, LicenseScope.SHIPPED),
        )
    }

    @Test
    fun `common spellings map to SPDX ids and ambiguous ones do not`() {
        val cases =
            mapOf(
                PomLicense("Apache License, Version 2.0", "") to "Apache-2.0",
                PomLicense("Apache-2.0", "") to "Apache-2.0",
                PomLicense("Apache License", "http://www.apache.org/licenses/LICENSE-2.0") to "Apache-2.0",
                PomLicense("The MIT License", "") to "MIT",
                PomLicense("BSD-3-Clause", "") to "BSD-3-Clause",
                PomLicense("New BSD License", "") to "BSD-3-Clause",
                PomLicense("The BSD 2-Clause License", "") to "BSD-2-Clause",
                PomLicense("ISC License", "") to "ISC",
                PomLicense("zlib License", "") to "Zlib",
                PomLicense("Eclipse Public License - v 1.0", "") to "EPL-1.0",
                PomLicense("Eclipse Public License v2.0", "") to "EPL-2.0",
                PomLicense("Apache License v2", "") to "Apache-2.0",
                PomLicense("BSD License", "") to null,
                PomLicense("The Apache Software License, Version 1.1", "") to null,
                PomLicense("GPL2 w/ CPE", "") to null,
            )

        assertEquals(cases, cases.keys.associateWith { spdxId(it) })
    }

    @Test
    fun `licenses are inherited from parent POMs`() {
        val poms =
            mapOf(
                "org.example:child:1.0" to parsePom(pom("", parent = "org.example:parent:1.0")),
                "org.example:parent:1.0" to parsePom(pom("", parent = "org.example:root:1.0")),
                "org.example:root:1.0" to parsePom(pom(license("Apache-2.0", ""))),
            )

        assertEquals(listOf(PomLicense("Apache-2.0", "")), licensesWithParents("org.example:child:1.0", poms::get))
    }

    @Test
    fun `configurations are classified as shipped, test or not distributed`() {
        val cases =
            mapOf(
                "runtimeClasspath" to LicenseScope.SHIPPED,
                "jvmRuntimeClasspath" to LicenseScope.SHIPPED,
                "androidRuntimeClasspath" to LicenseScope.SHIPPED,
                "releaseRuntimeClasspath" to LicenseScope.SHIPPED,
                "iosArm64CompileKlibraries" to LicenseScope.SHIPPED,
                "testRuntimeClasspath" to LicenseScope.TEST,
                "jvmTestRuntimeClasspath" to LicenseScope.TEST,
                "androidHostTestRuntimeClasspath" to LicenseScope.TEST,
                "debugUnitTestRuntimeClasspath" to LicenseScope.TEST,
                "iosSimulatorArm64TestCompileKlibraries" to LicenseScope.TEST,
                "debugRuntimeClasspath" to null,
                "devRuntimeClasspath" to null,
                "composeHotReloadDevRuntimeClasspath" to null,
                "jvmCompileClasspath" to null,
                "detekt" to null,
            )

        assertEquals(cases, cases.keys.associateWith { licenseScopeOf(it) })
    }

    private fun license(
        name: String,
        url: String,
    ) = "<license><name>$name</name><url>$url</url></license>"

    private fun pom(
        licenses: String,
        parent: String? = null,
    ): String {
        val parentXml =
            parent?.split(":")?.let { (g, a, v) -> "<parent><groupId>$g</groupId><artifactId>$a</artifactId><version>$v</version></parent>" }
        return """<?xml version="1.0"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              ${parentXml.orEmpty()}
              <artifactId>x</artifactId>
              <licenses>$licenses</licenses>
            </project>
        """.trimIndent()
    }
}
