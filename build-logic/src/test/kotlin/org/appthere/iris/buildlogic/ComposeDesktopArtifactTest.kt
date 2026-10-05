package org.appthere.iris.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComposeDesktopArtifactTest {
    @Test
    fun `maps each supported host to its compose desktop artifact`() {
        assertEquals("desktop-jvm-linux-x64", composeDesktopArtifact("Linux", "amd64"))
        assertEquals("desktop-jvm-linux-arm64", composeDesktopArtifact("Linux", "aarch64"))
        assertEquals("desktop-jvm-windows-x64", composeDesktopArtifact("Windows 11", "amd64"))
        assertEquals("desktop-jvm-windows-arm64", composeDesktopArtifact("Windows 11", "aarch64"))
        assertEquals("desktop-jvm-macos-x64", composeDesktopArtifact("Mac OS X", "x86_64"))
        assertEquals("desktop-jvm-macos-arm64", composeDesktopArtifact("Mac OS X", "aarch64"))
    }

    @Test
    fun `unsupported hosts fail with the host in the message`() {
        val os = assertFailsWith<IllegalStateException> { composeDesktopArtifact("FreeBSD", "amd64") }
        assertEquals(true, os.message?.contains("FreeBSD"))
        val arch = assertFailsWith<IllegalStateException> { composeDesktopArtifact("Linux", "riscv64") }
        assertEquals(true, arch.message?.contains("riscv64"))
    }
}
